package com.marketplace.listener;

import com.marketplace.event.CategoryReport;

import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/** Final link of the chain: keeps every inspection report that came back into the system. */
public final class ReportArchive {
    private final Queue<CategoryReport> reports = new ConcurrentLinkedQueue<>();

    public void onReport(CategoryReport report) {
        reports.add(report);
    }

    public List<CategoryReport> reports() {
        return List.copyOf(reports);
    }
}
