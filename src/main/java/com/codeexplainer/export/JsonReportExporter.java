package com.codeexplainer.export;

import com.codeexplainer.docs.ReportGenerator.AnalysisReport;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.OutputStream;

/**
 * Exports analysis reports to JSON format.
 */
@Component
public class JsonReportExporter implements ReportExporter {

    private final ObjectMapper objectMapper;

    public JsonReportExporter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void export(AnalysisReport report, OutputStream out) throws IOException {
        objectMapper.writerWithDefaultPrettyPrinter().writeValue(out, report);
    }

    @Override
    public ExportFormat getFormat() {
        return ExportFormat.JSON;
    }

    @Override
    public String getContentType() {
        return "application/json";
    }

    @Override
    public String getFileExtension() {
        return "json";
    }
}
