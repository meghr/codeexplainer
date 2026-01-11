package com.codeexplainer.export;

import com.codeexplainer.core.model.EndpointInfo;
import com.codeexplainer.detector.ComponentDetector.DetectedComponent;
import com.codeexplainer.docs.ReportGenerator.AnalysisReport;
import com.codeexplainer.docs.ReportGenerator.QualityIssue;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

/**
 * Exports analysis reports to PDF format.
 */
@Component
public class PdfReportExporter implements ReportExporter {

    @Override
    public void export(AnalysisReport report, OutputStream out) throws IOException {
        PdfWriter writer = new PdfWriter(out);
        PdfDocument pdf = new PdfDocument(writer);
        Document document = new Document(pdf);

        // Title
        document.add(new Paragraph("Code Explainer Analysis Report")
                .setFontSize(24)
                .setBold()
                .setTextAlignment(TextAlignment.CENTER));

        document.add(new Paragraph("JAR: " + report.jarName())
                .setFontSize(14)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginBottom(20));

        // Metrics Section
        document.add(new Paragraph("Metrics Summary").setFontSize(18).setBold());
        document.add(createMetricsTable(report));

        // Architecture Section
        document.add(new Paragraph("Architecture Overview").setFontSize(18).setBold().setMarginTop(20));

        // Components
        document.add(new Paragraph("Spring Components").setFontSize(14).setBold());
        if (report.architecture().components().isEmpty()) {
            document.add(new Paragraph("No components detected."));
        } else {
            document.add(createComponentTable(report.architecture().components()));
        }

        // Endpoints
        document.add(new Paragraph("REST Endpoints").setFontSize(14).setBold().setMarginTop(10));
        if (report.architecture().endpoints().isEmpty()) {
            document.add(new Paragraph("No REST endpoints detected."));
        } else {
            document.add(createEndpointTable(report.architecture().endpoints()));
        }

        // Quality Issues Section
        document.add(new Paragraph("Quality Issues").setFontSize(18).setBold().setMarginTop(20));
        if (report.qualityIssues().isEmpty()) {
            document.add(new Paragraph("No significant quality issues found."));
        } else {
            document.add(createQualityTable(report.qualityIssues()));
        }

        document.close();
    }

    private Table createMetricsTable(AnalysisReport report) {
        Table table = new Table(UnitValue.createPercentArray(new float[] { 1, 1 }));
        table.setWidth(UnitValue.createPercentValue(100));

        addMetricRow(table, "Total Classes", String.valueOf(report.classMetrics().totalClasses()));
        addMetricRow(table, "Interfaces", String.valueOf(report.classMetrics().interfaces()));
        addMetricRow(table, "Total Methods", String.valueOf(report.methodMetrics().totalMethods()));
        addMetricRow(table, "Avg Methods/Class", String.format("%.2f", report.methodMetrics().avgMethodsPerClass()));
        addMetricRow(table, "Max Inheritance Depth", String.valueOf(report.dependencyMetrics().maxInheritanceDepth()));
        addMetricRow(table, "Circular Dependencies", String.valueOf(report.dependencyMetrics().circularDependencies()));

        return table;
    }

    private void addMetricRow(Table table, String name, String value) {
        table.addCell(new Cell().add(new Paragraph(name).setBold()));
        table.addCell(new Cell().add(new Paragraph(value)));
    }

    private Table createComponentTable(List<DetectedComponent> components) {
        Table table = new Table(UnitValue.createPercentArray(new float[] { 1, 2, 1 }));
        table.setWidth(UnitValue.createPercentValue(100));

        table.addHeaderCell(
                new Cell().add(new Paragraph("Type").setBold().setBackgroundColor(ColorConstants.LIGHT_GRAY)));
        table.addHeaderCell(
                new Cell().add(new Paragraph("Name").setBold().setBackgroundColor(ColorConstants.LIGHT_GRAY)));
        table.addHeaderCell(
                new Cell().add(new Paragraph("Package").setBold().setBackgroundColor(ColorConstants.LIGHT_GRAY)));

        for (DetectedComponent comp : components) {
            table.addCell(new Cell().add(new Paragraph(comp.type().toString())));
            table.addCell(new Cell().add(new Paragraph(comp.className())));
            table.addCell(new Cell().add(new Paragraph(comp.packageName())));
        }

        return table;
    }

    private Table createEndpointTable(List<EndpointInfo> endpoints) {
        Table table = new Table(UnitValue.createPercentArray(new float[] { 1, 3 }));
        table.setWidth(UnitValue.createPercentValue(100));

        table.addHeaderCell(
                new Cell().add(new Paragraph("Method").setBold().setBackgroundColor(ColorConstants.LIGHT_GRAY)));
        table.addHeaderCell(
                new Cell().add(new Paragraph("Path").setBold().setBackgroundColor(ColorConstants.LIGHT_GRAY)));

        for (EndpointInfo ep : endpoints) {
            table.addCell(new Cell().add(new Paragraph(ep.getHttpMethod())));
            table.addCell(new Cell().add(new Paragraph(ep.getPath())));
        }

        return table;
    }

    private Table createQualityTable(List<QualityIssue> issues) {
        Table table = new Table(UnitValue.createPercentArray(new float[] { 1, 1, 3 }));
        table.setWidth(UnitValue.createPercentValue(100));

        table.addHeaderCell(
                new Cell().add(new Paragraph("Severity").setBold().setBackgroundColor(ColorConstants.LIGHT_GRAY)));
        table.addHeaderCell(
                new Cell().add(new Paragraph("Class").setBold().setBackgroundColor(ColorConstants.LIGHT_GRAY)));
        table.addHeaderCell(
                new Cell().add(new Paragraph("Message").setBold().setBackgroundColor(ColorConstants.LIGHT_GRAY)));

        for (QualityIssue issue : issues) {
            table.addCell(new Cell().add(new Paragraph(issue.severity().toString())));
            table.addCell(new Cell().add(new Paragraph(issue.className())));
            table.addCell(new Cell().add(new Paragraph(issue.message())));
        }

        return table;
    }

    @Override
    public ExportFormat getFormat() {
        return ExportFormat.PDF;
    }

    @Override
    public String getContentType() {
        return "application/pdf";
    }

    @Override
    public String getFileExtension() {
        return "pdf";
    }
}
