package com.dianping.cat.report.task.alert.summary;

import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.unidal.lookup.annotation.Inject;

import com.dianping.cat.Cat;
import com.dianping.cat.consumer.problem.ProblemAnalyzer;
import com.dianping.cat.consumer.problem.model.entity.ProblemReport;
import com.dianping.cat.report.page.PieChart.Item;
import com.dianping.cat.report.page.model.spi.ModelService;
import com.dianping.cat.report.page.problem.PieGraphChartVisitor;
import com.dianping.cat.report.page.problem.ProblemStatistics;
import com.dianping.cat.report.page.problem.ProblemStatistics.StatusStatistics;
import com.dianping.cat.report.page.problem.ProblemStatistics.TypeStatistics;
import com.dianping.cat.service.ModelRequest;
import com.dianping.cat.service.ModelResponse;

public class FailureModelGenerator {

	@Inject(type = ModelService.class, value = ProblemAnalyzer.ID)
	private ModelService<ProblemReport> m_service;

	private void addDistributeInfo(Map<Object, Object> resultMap, ProblemReport report) {
		PieGraphChartVisitor pieChart = new PieGraphChartVisitor("failure", null);
		Map<Object, Object> distributes = new HashMap<Object, Object>();

		pieChart.visitProblemReport(report);
		for (Item item : pieChart.getPieChart().getItems()) {
			distributes.put(item.getTitle(), item.getNumber());
		}
		resultMap.put("distributeMap", distributes);
	}

	private void addFailureInfo(Map<Object, Object> resultMap, ProblemReport report) {
		ProblemStatistics problemStatistics = new ProblemStatistics();
		problemStatistics.setAllIp(true);
		problemStatistics.visitProblemReport(report);
		TypeStatistics failureStatus = problemStatistics.getStatus().get("failure");

		if (failureStatus != null) {
			Map<Object, Object> statusMap = new HashMap<Object, Object>();

			for (StatusStatistics status : failureStatus.getStatus().values()) {
				statusMap.put(status.getStatus(), status.getCount());
			}

			resultMap.put("count", failureStatus.getCount());
			resultMap.put("statusMap", statusMap);
		}
	}

	public Map<Object, Object> generateFailureModel(String domain, Date endTime) {
		Map<Object, Object> result = new HashMap<Object, Object>();
		ModelRequest request = new ModelRequest(domain, getCurrentHour()).setProperty("queryType", "view");
		ProblemReport report = null;

		if (m_service.isEligable(request)) {
			ModelResponse<ProblemReport> response = m_service.invoke(request);
			report = response.getModel();
		}

		try {
			addFailureInfo(result, report);
			addDistributeInfo(result, report);
		} catch (Exception ex) {
			Cat.logError(ex);
		}
		return result;
	}

	private long getCurrentHour() {
		Calendar cal = Calendar.getInstance();
		cal.set(Calendar.MINUTE, 0);
		cal.set(Calendar.SECOND, 0);
		cal.set(Calendar.MILLISECOND, 0);

		return cal.getTimeInMillis();
	}
}
