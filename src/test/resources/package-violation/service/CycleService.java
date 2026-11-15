package piems.service;

import piems.report.CycleReport;

/**
 * Deliberate violation: CycleService depends on CycleReport.
 */
public class CycleService {
    private CycleReport report;
}
