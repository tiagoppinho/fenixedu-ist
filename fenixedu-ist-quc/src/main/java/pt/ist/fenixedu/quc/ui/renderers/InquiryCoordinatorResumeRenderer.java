/**
 * Copyright © 2013 Instituto Superior Técnico
 *
 * This file is part of FenixEdu IST QUC.
 *
 * FenixEdu IST QUC is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * FenixEdu IST QUC is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with FenixEdu IST QUC.  If not, see <http://www.gnu.org/licenses/>.
 */
/**
 * 
 */
package pt.ist.fenixedu.quc.ui.renderers;

import javax.servlet.http.HttpSession;

import org.apache.commons.lang.StringUtils;
import org.fenixedu.academic.domain.ExecutionCourse;
import org.fenixedu.academic.domain.ExecutionDegree;
import org.fenixedu.academic.domain.Person;
import org.fenixedu.academic.domain.Professorship;

import pt.ist.fenixWebFramework.renderers.components.HtmlInlineContainer;
import pt.ist.fenixWebFramework.renderers.components.HtmlLink;
import pt.ist.fenixWebFramework.renderers.components.HtmlMenu;
import pt.ist.fenixWebFramework.renderers.components.HtmlMenuGroup;
import pt.ist.fenixWebFramework.renderers.components.HtmlMenuOption;
import pt.ist.fenixWebFramework.renderers.components.HtmlTableCell;
import pt.ist.fenixWebFramework.renderers.components.HtmlTableRow;
import pt.ist.fenixWebFramework.renderers.components.HtmlText;
import pt.ist.fenixWebFramework.renderers.utils.RenderUtils;
import pt.ist.fenixWebFramework.servlets.filters.contentRewrite.GenericChecksumRewriter;
import pt.ist.fenixedu.quc.domain.InquiryDelegateAnswer;
import pt.ist.fenixedu.quc.domain.InquiryGlobalComment;
import pt.ist.fenixedu.quc.domain.InquiryResultComment;
import pt.ist.fenixedu.quc.domain.ResultClassification;
import pt.ist.fenixedu.quc.domain.ResultPersonCategory;
import pt.ist.fenixedu.quc.dto.BlockResumeResult;
import pt.ist.fenixedu.quc.dto.CurricularCourseResumeResult;
import pt.ist.fenixedu.quc.dto.TeacherShiftTypeResultsBean;

/**
 * @author - Ricardo Rodrigues (ricardo.rodrigues@ist.utl.pt)
 * 
 */
public class InquiryCoordinatorResumeRenderer extends InquiryBlocksResumeRenderer {

    private String contextPath;
    private String action;
    private String method;

    @Override
    protected void createFinalCells(HtmlTableRow tableRow, BlockResumeResult blockResumeResult) {
        CurricularCourseResumeResult courseResumeResult = (CurricularCourseResumeResult) blockResumeResult;

        HtmlInlineContainer container = new HtmlInlineContainer();
        HtmlTableCell linksCell = tableRow.createCell();
        HtmlMenu menu = new HtmlMenu();
        menu.setOnChange("var value=this.options[this.selectedIndex].value; this.selectedIndex=0; if(value!= ''){ window.open(value,'_blank'); }");
        menu.setStyle("width: 150px");
        HtmlMenuOption optionEmpty =
                menu.createOption(RenderUtils.getResourceString("INQUIRIES_RESOURCES", "label.inquiry.emptyOption"));

        createResultsGroup(courseResumeResult, menu);
        createReportsGroup(courseResumeResult, menu);

        container.addChild(menu);
        ResultClassification forAudit =
                ResultClassification
                        .getForAudit(courseResumeResult.getExecutionCourse(), courseResumeResult.getExecutionDegree());
        createCommentLink(courseResumeResult, container, forAudit);

        linksCell.setBody(container);
        linksCell.setClasses("col-actions");
    }

    protected void createCommentLink(CurricularCourseResumeResult courseResumeResult, HtmlInlineContainer container,
            ResultClassification forAudit) {
        container.addChild(new HtmlText("&nbsp;|&nbsp;", false));

        String commentLinkText = RenderUtils.getResourceString("INQUIRIES_RESOURCES", "link.inquiry.comment");
        if (courseResumeResult.isShowAllComments() || !courseResumeResult.isAllowComment()) {
            commentLinkText = RenderUtils.getResourceString("INQUIRIES_RESOURCES", "link.inquiry.viewResults");
        } else if (hasQucGlobalCommentsMadeBy(courseResumeResult.getExecutionCourse(), courseResumeResult.getPerson(),
                courseResumeResult.getExecutionDegree(), courseResumeResult.getPersonCategory())) {
            commentLinkText = RenderUtils.getResourceString("INQUIRIES_RESOURCES", "link.inquiry.viewComment");
        }

        String fillInParameters = buildFillInParameters(courseResumeResult);
        HtmlLink commentLink = new HtmlLink();
        commentLink.setUrl(getAction() + "?method=" + getMethod() + fillInParameters);
        commentLink.setText(commentLinkText);
        container.addChild(commentLink);

        if (courseResumeResult.getExecutionCourse().getExecutionCourseAudit() != null
                && courseResumeResult.getExecutionCourse().getExecutionCourseAudit().isProcessAvailable()) {
            container.addChild(new HtmlText("&nbsp;|&nbsp;", false));
            HtmlLink auditLink = new HtmlLink();
            auditLink.setUrl("/auditResult.do?method=viewProcessDetails&" + buildParametersForAudit(courseResumeResult));
            auditLink.setText("Ver processo auditoria");
            container.addChild(auditLink);

        } else {
            if (forAudit != null) {
                if (forAudit.equals(ResultClassification.RED)) {
                    container.addChild(new HtmlText(" ("
                            + RenderUtils.getResourceString("INQUIRIES_RESOURCES", "label.inquiry.audit") + ")"));
                } else if (forAudit.equals(ResultClassification.YELLOW)) {
                    container.addChild(new HtmlText(" ("
                            + RenderUtils.getResourceString("INQUIRIES_RESOURCES", "label.inquiry.inObservation") + ")"));
                }
            }
        }
    }

    private boolean hasQucGlobalCommentsMadeBy(ExecutionCourse executionCourse, Person person, ExecutionDegree executionDegree,
            ResultPersonCategory personCategory) {
        InquiryGlobalComment inquiryGlobalComment =
                InquiryGlobalComment.getInquiryGlobalComment(executionCourse, executionDegree);
        if (inquiryGlobalComment != null) {
            for (InquiryResultComment resultComment : inquiryGlobalComment.getInquiryResultCommentsSet()) {
                if (resultComment.getPerson() == person && personCategory.equals(resultComment.getPersonCategory())
                        && !StringUtils.isEmpty(resultComment.getComment())) {
                    return true;
                }
            }
        }
        return false;
    }

    private void createResultsGroup(CurricularCourseResumeResult courseResumeResult, HtmlMenu menu) {
        HtmlMenuGroup resultsGroup =
                menu.createGroup(RenderUtils.getResourceString("INQUIRIES_RESOURCES", "label.inquiry.results"));
        HtmlMenuOption optionUC = resultsGroup.createOption();
        optionUC.setText(RenderUtils.getResourceString("INQUIRIES_RESOURCES", "label.inquiry.ucResults"));
        String resultsParameters = buildParametersForResults(courseResumeResult);
        HtmlLink link = new HtmlLink();
        link.setModule("/publico");
        link.setUrl("/viewCourseResults.do?" + resultsParameters);
        link.setEscapeAmpersand(false);
        String calculatedUrl = link.calculateUrl();
        optionUC.setValue(calculatedUrl + "&_request_checksum_="
                + GenericChecksumRewriter.calculateChecksum(calculatedUrl, getSession()));

        for (TeacherShiftTypeResultsBean teacherShiftTypeResultsBean : courseResumeResult.getTeachersResults()) {
            String teacherResultsParameters = buildParametersForTeacherResults(teacherShiftTypeResultsBean);
            HtmlLink teacherLink = new HtmlLink();
            teacherLink.setEscapeAmpersand(false);
            teacherLink.setModule("/publico");
            teacherLink.setUrl("/viewTeacherResults.do?" + teacherResultsParameters);
            calculatedUrl = teacherLink.calculateUrl();

            HtmlMenuOption optionTeacher = resultsGroup.createOption();
            optionTeacher.setText(teacherShiftTypeResultsBean.getShiftType().getFullNameTipoAula() + " - "
                    + teacherShiftTypeResultsBean.getProfessorship().getPerson().getName());
            optionTeacher.setValue(calculatedUrl + "&_request_checksum_="
                    + GenericChecksumRewriter.calculateChecksum(calculatedUrl, getSession()));
        }
    }

    private void createReportsGroup(CurricularCourseResumeResult courseResumeResult, HtmlMenu menu) {
        String calculatedUrl;
        HtmlMenuGroup reportsGroup =
                menu.createGroup(RenderUtils.getResourceString("INQUIRIES_RESOURCES", "label.inquiry.reports"));
        for (InquiryDelegateAnswer inquiryDelegateAnswer : courseResumeResult.getExecutionCourse()
                .getInquiryDelegatesAnswersSet()) {
            if (inquiryDelegateAnswer.getExecutionDegree() == courseResumeResult.getExecutionDegree()) {
                String delegateInquiryParameters = buildParametersForDelegateInquiry(inquiryDelegateAnswer);
                HtmlLink delegateLink = new HtmlLink();
                delegateLink.setEscapeAmpersand(false);
                delegateLink.setModule("/publico");
                delegateLink.setUrl("/viewQUCInquiryAnswers.do?" + delegateInquiryParameters);
                calculatedUrl = delegateLink.calculateUrl();

                HtmlMenuOption optionDelegate = reportsGroup.createOption();
                optionDelegate.setText(RenderUtils.getResourceString("INQUIRIES_RESOURCES", "label.inquiry.delegate"));
                optionDelegate.setValue(calculatedUrl + "&_request_checksum_="
                        + GenericChecksumRewriter.calculateChecksum(calculatedUrl, getSession()));
            }
        }

        for (Professorship professorship : courseResumeResult.getExecutionCourse().getProfessorshipsSet()) {
            if (professorship.getInquiryTeacherAnswer() != null) {
                HtmlLink teacherLink = new HtmlLink();
                teacherLink.setEscapeAmpersand(false);
                teacherLink.setModule("/publico");
                teacherLink.setUrl("/viewQUCInquiryAnswers.do?method=showTeacherInquiry&professorshipOID="
                        + professorship.getExternalId());
                calculatedUrl = teacherLink.calculateUrl();
                HtmlMenuOption optionTeacher = reportsGroup.createOption();
                optionTeacher.setText(RenderUtils.getResourceString("INQUIRIES_RESOURCES", "label.teacher") + " ("
                        + professorship.getPerson().getName() + ")");
                optionTeacher.setValue(calculatedUrl + "&_request_checksum_="
                        + GenericChecksumRewriter.calculateChecksum(calculatedUrl, getSession()));
            }
        }

        for (Professorship professorship : courseResumeResult.getExecutionCourse().getProfessorshipsSet()) {
            if (professorship.getInquiryRegentAnswer() != null) {
                HtmlLink regentLink = new HtmlLink();
                regentLink.setEscapeAmpersand(false);
                regentLink.setModule("/publico");
                regentLink.setUrl("/viewQUCInquiryAnswers.do?method=showRegentInquiry&professorshipOID="
                        + professorship.getExternalId());
                calculatedUrl = regentLink.calculateUrl();
                HtmlMenuOption optionRegent = reportsGroup.createOption();
                optionRegent.setText(RenderUtils.getResourceString("INQUIRIES_RESOURCES", "label.inquiry.regent") + " ("
                        + professorship.getPerson().getName() + ")");
                optionRegent.setValue(calculatedUrl + "&_request_checksum_="
                        + GenericChecksumRewriter.calculateChecksum(calculatedUrl, getSession()));
            }
        }
    }

    private HttpSession getSession() {
        return getContext().getViewState().getRequest().getSession();
    }

    private String buildParametersForDelegateInquiry(InquiryDelegateAnswer inquiryDelegateAnswer) {
        StringBuilder builder = new StringBuilder("method=showDelegateInquiry");
        builder.append("&executionCourseOID=").append(inquiryDelegateAnswer.getExecutionCourse().getExternalId());
        builder.append("&executionDegreeOID=").append(inquiryDelegateAnswer.getExecutionDegree().getExternalId());
        return builder.toString();
    }

    private String buildParametersForTeacherResults(TeacherShiftTypeResultsBean teacherShiftTypeResultsBean) {
        StringBuilder builder = new StringBuilder();
        builder.append("shiftType=").append(teacherShiftTypeResultsBean.getShiftType().name());
        builder.append("&professorshipOID=").append(teacherShiftTypeResultsBean.getProfessorship().getExternalId());
        return builder.toString();
    }

    private String buildFillInParameters(CurricularCourseResumeResult courseResumeResult) {
        StringBuilder builder = new StringBuilder();
        builder.append("&executionDegreeOID=").append(courseResumeResult.getExecutionDegree().getExternalId());
        builder.append("&executionCourseOID=").append(courseResumeResult.getExecutionCourse().getExternalId());
        builder.append("&degreeCurricularPlanID=").append(
                courseResumeResult.getExecutionDegree().getDegreeCurricularPlan().getExternalId());
        builder.append("&backToResume=").append(courseResumeResult.isBackToResume());
        builder.append("&showAllComments=").append(courseResumeResult.isShowAllComments());
        builder.append("&allowComment=").append(courseResumeResult.isAllowComment());
        return builder.toString();
    }

    private String buildParametersForResults(CurricularCourseResumeResult courseResumeResult) {
        StringBuilder builder = new StringBuilder();
        builder.append("degreeCurricularPlanOID=").append(
                courseResumeResult.getExecutionDegree().getDegreeCurricularPlan().getExternalId());
        builder.append("&executionCourseOID=").append(courseResumeResult.getExecutionCourse().getExternalId());
        return builder.toString();
    }

    private String buildParametersForAudit(CurricularCourseResumeResult courseResumeResult) {
        StringBuilder builder = new StringBuilder();
        builder.append("degreeCurricularPlanOID=").append(
                courseResumeResult.getExecutionDegree().getDegreeCurricularPlan().getExternalId());
        builder.append("&executionCourseAuditOID=").append(
                courseResumeResult.getExecutionCourse().getExecutionCourseAudit().getExternalId());
        return builder.toString();
    }

    public String getContextPath() {
        return contextPath;
    }

    public void setContextPath(String contextPath) {
        this.contextPath = contextPath;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }
}