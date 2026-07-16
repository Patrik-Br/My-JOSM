package org.openstreetmap.josm.plugins.mapathonqa;
import java.awt.BorderLayout; import java.awt.event.ActionEvent;
import java.util.*; import javax.swing.*;
import org.openstreetmap.josm.data.osm.*;
import org.openstreetmap.josm.gui.MainApplication; import org.openstreetmap.josm.tools.I18n;
public class CheckOverlappingBuildingsAction extends AbstractAction {
    public CheckOverlappingBuildingsAction() { super(I18n.tr("Check: Overlapping Buildings")); }
    @Override public void actionPerformed(ActionEvent e) {
        DataSet ds = MainApplication.getLayerManager().getEditDataSet();
        if (ds == null) { JOptionPane.showMessageDialog(null, "No active OSM data layer found.", "MapathonQA", JOptionPane.WARNING_MESSAGE); return; }
        JDialog prog = CheckNonYesBuildingTagsAction.makeProgress("Checking overlapping buildings...");
        prog.setVisible(true);
        new SwingWorker<Set<OsmPrimitive>, Void>() {
            @Override protected Set<OsmPrimitive> doInBackground() { return runOn(ds, null, null); }
            @Override protected void done() {
                prog.dispose();
                try {
                    Set<OsmPrimitive> f = get();
                    if (f.isEmpty()) { JOptionPane.showMessageDialog(null, "No overlapping buildings found.", "MapathonQA", JOptionPane.INFORMATION_MESSAGE); return; }
                    ds.setSelected(f);
                    JOptionPane.showMessageDialog(null, I18n.tr("{0} building(s) overlap with another building.\n\nThey are now selected.", f.size()), "MapathonQA \u2013 Overlapping Buildings", JOptionPane.WARNING_MESSAGE);
                } catch (Exception ex) { prog.dispose(); JOptionPane.showMessageDialog(null, "Check failed:\n"+ex.getMessage(), "MapathonQA", JOptionPane.ERROR_MESSAGE); }
            }
        }.execute();
    }
    public static Set<OsmPrimitive> runOn(DataSet ds) { return runOn(ds, null, null); }
    public static Set<OsmPrimitive> runOn(DataSet ds, java.util.Date since) { return runOn(ds, since, null); }
    public static Set<OsmPrimitive> runOn(DataSet ds, java.util.Date since, java.util.Date until) {
        List<Way> buildings = new ArrayList<>();
        for (Way w : ds.getWays()) if (w.isClosed()&&w.hasKey("building")&&w.getNodesCount()>=4) buildings.add(w);
        Set<OsmPrimitive> f = new LinkedHashSet<>();
        int n=buildings.size();
        for (int i=0;i<n;i++) { Way a=buildings.get(i); for (int j=i+1;j<n;j++) { Way b=buildings.get(j);
            if (GeometryUtil.bboxDisjoint(a,b)||GeometryUtil.waysShareNode(a,b)) continue;
            if (GeometryUtil.waysOverlap(a,b)) { if (GeometryUtil.isMappedDuring(a,since,until)) f.add(a); else if (GeometryUtil.isMappedDuring(b,since,until)) f.add(b); }
        }}
        return f;
    }
}
