package com.codeexplainer.export;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Service to handle report exports.
 */
@Service
public class ExportService {

    private final Map<ReportExporter.ExportFormat, ReportExporter> exporters;

    public ExportService(List<ReportExporter> exporterList) {
        this.exporters = exporterList.stream()
                .collect(Collectors.toMap(ReportExporter::getFormat, e -> e));
    }

    /**
     * Gets the exporter for the specified format.
     *
     * @param format The export format
     * @return The exporter, or default (JSON) if not found
     */
    public ReportExporter getExporter(String format) {
        ReportExporter.ExportFormat exportFormat;
        try {
            exportFormat = ReportExporter.ExportFormat.valueOf(format.toUpperCase());
        } catch (IllegalArgumentException | NullPointerException e) {
            exportFormat = ReportExporter.ExportFormat.JSON;
        }

        return exporters.getOrDefault(exportFormat, exporters.get(ReportExporter.ExportFormat.JSON));
    }
}
