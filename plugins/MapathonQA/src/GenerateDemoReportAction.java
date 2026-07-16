package org.openstreetmap.josm.plugins.mapathonqa;

import java.awt.event.ActionEvent;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.AbstractSet;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.TimeZone;

import javax.swing.AbstractAction;
import javax.swing.JOptionPane;

import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.tools.I18n;

/**
 * Generates a demo HTML report with 100 issues in every check category.
 * Useful for showing colleagues what a full report looks like.
 */
public class GenerateDemoReportAction extends AbstractAction {

    public GenerateDemoReportAction() {
        super(I18n.tr("Generate Demo Report"));
    }

    /**
     * A Set that always reports a fixed size, regardless of contents.
     * Used for demo purposes to show 100 issues in Set-typed check results
     * without deduplication collapsing multiple nulls into one.
     */
    private static class FixedSizeSet extends AbstractSet<OsmPrimitive> {
        private final int fixedSize;
        FixedSizeSet(int size) { this.fixedSize = size; }
        @Override public int size() { return fixedSize; }
        @Override public boolean isEmpty() { return fixedSize == 0; }
        @Override public Iterator<OsmPrimitive> iterator() { return new java.util.ArrayList<OsmPrimitive>().iterator(); }
        @Override public boolean add(OsmPrimitive e) { return false; }
        @Override public boolean addAll(Collection<? extends OsmPrimitive> c) { return false; }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        QAResults r = new QAResults();
        r.projectId = 12345;
        r.startTime = "2026-01-01 09:00";
        r.endTime   = "2026-01-01 13:00";

        // Set since/until so "last edited during mapathon" counts appear
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");
            sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
            r.since = sdf.parse("2026-01-01 09:00");
            r.until = sdf.parse("2026-01-01 13:00");
        } catch (Exception ex) { /* ignore */ }

        r.totalBuildings  = 300;
        r.totalHighways   = 300;
        r.totalNodes      = 18340;
        r.totalWays       = 2589;
        r.totalRelations  = 12;
        r.totalIssueOverride = 314;
        r.mapathonBuildings = 100;
        r.mapathonHighways  = 100;

        // 100 issues in every check
        // Lists allow multiple nulls → size() = 100 ✓
        for (int i = 0; i < 314; i++) r.nonYesBuildingTags.add(null);
        for (int i = 0; i < 314; i++) r.nonOrthogonalBuildings.add(null);
        for (int i = 0; i < 314; i++) r.buildingsWithLayerTag.add(null);
        for (int i = 0; i < 314; i++) r.untaggedWays.add(null);

        // Sets deduplicate nulls → use FixedSizeSet to show 100 ✓
        r.overlappingBuildings = new FixedSizeSet(314);
        r.buildingsOnHighways  = new FixedSizeSet(314);

        // Shared nodes: 100 shared nodes, 100 affected buildings
        r.buildingsWithSharedNodes = new CheckBuildingsWithSharedNodesAction.SharedNodeResult(
            314, new FixedSizeSet(314)
        );

        try {
            File report = ReportWriter.writeDemoReport(r);
            JOptionPane.showMessageDialog(null,
                "Demo report saved to:\n" + report.getAbsolutePath()
                + "\n\nThis is a demonstration report with 100 simulated issues\n"
                + "in each category. Not based on real OSM data.",
                "MapathonQA \u2013 Demo Report",
                JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(null,
                "Could not write demo report:\n" + ex.getMessage(),
                "MapathonQA", JOptionPane.ERROR_MESSAGE);
        }
    }
}
