/**
 * Copyright © 2013 Instituto Superior Técnico
 *
 * This file is part of FenixEdu IST Vigilancies.
 *
 * FenixEdu IST Vigilancies is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * FenixEdu IST Vigilancies is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with FenixEdu IST Vigilancies.  If not, see <http://www.gnu.org/licenses/>.
 */
package pt.ist.fenixedu.vigilancies.ui.struts.action.vigilancy;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.fenixedu.academic.domain.Department;
import org.fenixedu.academic.domain.ExecutionCourse;
import org.fenixedu.academic.domain.exceptions.DomainException;
import org.fenixedu.academic.domain.organizationalStructure.ScientificAreaUnit;
import org.fenixedu.academic.domain.organizationalStructure.Unit;
import org.fenixedu.academic.ui.struts.action.base.FenixDispatchAction;
import org.fenixedu.bennu.struts.annotations.Forward;
import org.fenixedu.bennu.struts.annotations.Forwards;
import org.fenixedu.bennu.struts.annotations.Mapping;

import pt.ist.fenixWebFramework.renderers.components.state.IViewState;
import pt.ist.fenixWebFramework.renderers.utils.RenderUtils;
import pt.ist.fenixedu.vigilancies.domain.VigilantGroup;
import pt.ist.fenixedu.vigilancies.service.AddExecutionCourseToGroup;
import pt.ist.fenixedu.vigilancies.service.RemoveExecutionCoursesFromGroup;
import pt.ist.fenixframework.FenixFramework;

@Mapping(module = "examCoordination", path = "/vigilancy/vigilancyCourseGroupManagement",
        functionality = VigilantGroupManagement.class)
@Forwards(value = { @Forward(name = "editCourseGroup", path = "/examCoordinator/vigilancy/editVigilancyCourseGroup.jsp") })
public class VigilancyCourseGroupManagement extends FenixDispatchAction {

    public ActionForward prepareEdition(ActionMapping mapping, ActionForm form, HttpServletRequest request,
            HttpServletResponse response) throws Exception {

        VigilancyCourseGroupBean bean = new VigilancyCourseGroupBean();
        String oid = request.getParameter("gid");

        VigilantGroup group = (VigilantGroup) FenixFramework.getDomainObject(oid);
        bean.setSelectedVigilantGroup(group);
        bean.setSelectedDepartment(getDepartment(group));
        request.setAttribute("bean", bean);

        return mapping.findForward("editCourseGroup");
    }

    private Department getDepartment(VigilantGroup group) {
        Unit unit = group.getUnit();
        if (unit.isDepartmentUnit()) {
            return unit.getDepartment();
        }
        if (unit.isScientificAreaUnit()) {
            ScientificAreaUnit scientificAreaUnit = (ScientificAreaUnit) unit;
            return scientificAreaUnit.getDepartmentUnit().getDepartment();
        }
        return null;
    }

    public ActionForward selectUnit(ActionMapping mapping, ActionForm form, HttpServletRequest request,
            HttpServletResponse response) throws Exception {

        VigilancyCourseGroupBean bean =
                (VigilancyCourseGroupBean) RenderUtils.getViewState("selectUnit").getMetaObject().getObject();
        request.setAttribute("bean", bean);
        RenderUtils.invalidateViewState("selectUnit");
        return mapping.findForward("editCourseGroup");
    }

    public ActionForward addExecutionCourseToGroup(ActionMapping mapping, ActionForm form, HttpServletRequest request,
            HttpServletResponse response) throws Exception {

        IViewState viewState = RenderUtils.getViewState("addExecutionCourses");
        VigilancyCourseGroupBean bean = (VigilancyCourseGroupBean) viewState.getMetaObject().getObject();
        List<ExecutionCourse> executionCourses = bean.getCoursesToAdd();
        VigilantGroup group = bean.getSelectedVigilantGroup();

        if (executionCourses.size() > 0) {
            List<ExecutionCourse> coursesUnableToAdd;

            coursesUnableToAdd = AddExecutionCourseToGroup.run(group, executionCourses);

            request.setAttribute("coursesUnableToAdd", coursesUnableToAdd);
        }
        bean.setCoursesToAdd(new ArrayList<ExecutionCourse>());
        request.setAttribute("bean", bean);
        RenderUtils.invalidateViewState("addExecutionCourses");
        return mapping.findForward("editCourseGroup");

    }

    public ActionForward removeExecutionCoursesFromGroup(ActionMapping mapping, ActionForm form, HttpServletRequest request,
            HttpServletResponse response) throws Exception {

        VigilancyCourseGroupBean bean =
                (VigilancyCourseGroupBean) RenderUtils.getViewState("removeExecutionCourses").getMetaObject().getObject();
        List<ExecutionCourse> executionCourses = bean.getCourses();
        VigilantGroup group = bean.getSelectedVigilantGroup();

        try {

            RemoveExecutionCoursesFromGroup.run(group, executionCourses);
        } catch (DomainException e) {
            addActionMessage(request, e.getMessage());
        }

        request.setAttribute("bean", bean);
        RenderUtils.invalidateViewState("removeExecutionCourses");
        return mapping.findForward("editCourseGroup");
    }

    public ActionForward addExternalCourse(ActionMapping mapping, ActionForm form, HttpServletRequest request,
            HttpServletResponse response) throws Exception {

        IViewState viewState = RenderUtils.getViewState("addExternalCourse");
        if (viewState == null) {
            viewState = RenderUtils.getViewState("addExternalCourse-withoutjs");
        }
        VigilancyCourseGroupBean bean = (VigilancyCourseGroupBean) viewState.getMetaObject().getObject();

        ExecutionCourse course = bean.getExternalCourse();
        VigilantGroup group = bean.getSelectedVigilantGroup();
        if (course != null) {
            List<ExecutionCourse> courses = new ArrayList<ExecutionCourse>();
            courses.add(course);

            try {

                AddExecutionCourseToGroup.run(group, courses);
            } catch (DomainException e) {
                addActionMessage(request, e.getMessage());
            }
        }
        if (RenderUtils.getViewState("addExternalCourse-withoutjs") != null) {
            RenderUtils.invalidateViewState("addExternalCourse-withoutjs");
        }

        request.setAttribute("bean", bean);
        bean.setExternalCourse(null);
        return mapping.findForward("editCourseGroup");
    }
}