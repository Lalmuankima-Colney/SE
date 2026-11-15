package piems.report;

import piems.service.CycleService;

/**
 * Deliberate violation: CycleReport depends on CycleService, creating a cycle.
 */
public class CycleReport {
    private CycleService service;
}
