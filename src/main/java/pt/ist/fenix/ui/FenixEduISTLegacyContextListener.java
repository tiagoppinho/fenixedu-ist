/**
 * Copyright © ${project.inceptionYear} Instituto Superior Técnico
 *
 * This file is part of Fenix IST.
 *
 * Fenix IST is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Fenix IST is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Fenix IST.  If not, see <http://www.gnu.org/licenses/>.
 */
package pt.ist.fenix.ui;

import java.util.HashSet;
import java.util.Set;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;

import org.fenixedu.academic.domain.Degree;
import org.fenixedu.academic.domain.DegreeCurricularPlan;
import org.fenixedu.academic.domain.ExecutionDegree;
import org.fenixedu.academic.domain.ExecutionYear;
import org.fenixedu.academic.domain.Person;
import org.fenixedu.academic.domain.organizationalStructure.Unit;
import org.fenixedu.academic.thesis.domain.ThesisProposal;
import org.fenixedu.academic.thesis.domain.ThesisProposalsConfiguration;
import org.fenixedu.academic.util.Bundle;
import org.fenixedu.bennu.core.i18n.BundleUtil;
import org.fenixedu.bennu.signals.DomainObjectEvent;
import org.fenixedu.bennu.signals.Signal;
import org.fenixedu.cms.domain.Category;

import pt.ist.fenixframework.Atomic;
import pt.ist.fenixframework.Atomic.TxMode;
import pt.ist.fenixframework.FenixFramework;

import com.google.common.eventbus.Subscribe;

@WebListener
public class FenixEduISTLegacyContextListener implements ServletContextListener {
    @Override
    public void contextInitialized(ServletContextEvent sce) {
        FenixFramework.getDomainModel().registerDeletionBlockerListener(Person.class, (person, blockers) -> {
            if (!person.getPersistentGroupsSet().isEmpty()) {
                blockers.add(BundleUtil.getString(Bundle.APPLICATION, "error.person.cannot.be.deleted"));
            }
        });
        FenixFramework.getDomainModel().registerDeletionBlockerListener(Unit.class, (unit, blockers) -> {
            if (!unit.getFilesSet().isEmpty() || !unit.getPersistentGroupsSet().isEmpty()) {
                blockers.add(BundleUtil.getString(Bundle.APPLICATION, "error.unit.cannot.be.deleted"));
            }
        });

        FenixFramework.getDomainModel().registerDeletionListener(Unit.class, unit -> {
            for (; !unit.getUnitFileTagsSet().isEmpty(); unit.getUnitFileTagsSet().iterator().next().delete()) {
                ;
            }
        });

        FenixFramework.getDomainModel().registerDeletionListener(Category.class, cat -> {
            cat.getBookmarkedBySet().clear();
        });

        Signal.register(ThesisProposal.SIGNAL_CREATED, new FenixEduISTLegacyContextListener());

    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
    }

    @Subscribe
    @Atomic(mode = TxMode.WRITE)
    public void propagateThesisProposal(DomainObjectEvent<ThesisProposal> e) {
        ThesisProposal thesisProposal = e.getInstance();

        Set<ThesisProposalsConfiguration> configs = new HashSet<ThesisProposalsConfiguration>();

        for (ExecutionDegree executionDegree : thesisProposal.getExecutionDegreeSet()) {

            Degree degree = executionDegree.getDegree();
            ExecutionYear executionYear = executionDegree.getExecutionYear();

            DegreeCurricularPlan meec = DegreeCurricularPlan.readByNameAndDegreeSigla("MEEC 2006", "MEEC");
            DegreeCurricularPlan mee = DegreeCurricularPlan.readByNameAndDegreeSigla("MEE 2006", "MEE");
            DegreeCurricularPlan meicT = DegreeCurricularPlan.readByNameAndDegreeSigla("MEIC-T 2006", "MEIC-T");
            DegreeCurricularPlan meicA = DegreeCurricularPlan.readByNameAndDegreeSigla("MEIC-A 2006", "MEIC-A");

            if (degree.getSigla().equals("MEEC")) { // MEEC -> MEE
                ExecutionDegree meeExecDegree = mee.getExecutionDegreeByYear(executionYear);

                configs.addAll(ThesisProposalsConfiguration.getConfigurationsWithOpenProposalPeriod(meeExecDegree));
            }

            if (degree.getSigla().equals("MEE")) { // MEE -> MEEC
                ExecutionDegree meecExecDegree = meec.getExecutionDegreeByYear(executionYear);

                configs.addAll(ThesisProposalsConfiguration.getConfigurationsWithOpenProposalPeriod(meecExecDegree));
            }

            if (degree.getSigla().equals("MEIC-T")) { // MEIC-T ->
                // MEIC-A
                ExecutionDegree meicAExecDegree = meicA.getExecutionDegreeByYear(executionYear);

                configs.addAll(ThesisProposalsConfiguration.getConfigurationsWithOpenProposalPeriod(meicAExecDegree));
            }

            if (degree.getSigla().equals("MEIC-A")) { // MEIC-A ->
                // MEIC-T
                ExecutionDegree meicTExecDegree = meicT.getExecutionDegreeByYear(executionYear);

                configs.addAll(ThesisProposalsConfiguration.getConfigurationsWithOpenProposalPeriod(meicTExecDegree));
            }
        }

        thesisProposal.getThesisConfigurationSet().addAll(configs);
    }
}