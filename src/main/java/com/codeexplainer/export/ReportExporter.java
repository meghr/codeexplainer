package com.codeexplainer.export;

import com.codeexplainer.docs.ReportGenerator.AnalysisReport;

import java.io.IOException;
import java.io.OutputStream;

/**
 * Interface for exporting analysis reports.
 */
public interface ReportExporter {

    /**
     * Supported export formats.
     */
    enum ExportFormat {
        PDF,
        JSON,
        MARKDOWN
    }

    /**
     * Exports the analysis report to the given output stream.
     *
     * @param report The analysis report to export
     * @param out    The output stream to write to
     * @throws IOException If an I/O error occurs
     */
    void export(AnalysisReport report, OutputStream out) throws IOException;

    /**
     * Gets the format this exporter supports.
     *
     * @return The export format
     */
    ExportFormat getFormat();

    /**
     * Gets the MIME type for the exported content.
     * 
     * @return The MIME type string
     */
    String getContentType();

    /**
     * Gets the file extension for the exported content.
     * 
     * @return The file extension (without dot)
     */
    String getFileExtension();
}
