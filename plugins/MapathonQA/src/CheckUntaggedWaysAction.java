package org.openstreetmap.josm.plugins.mapathonqa;
import java.awt.BorderLayout; import java.awt.event.ActionEvent;
import java.util.*; import javax.swing.*;
import org.openstreetmap.josm.data.osm.*;
import org.openstreetmap.josm.gui.MainApplication; import org.openstreetmap.josm.tools.I18n;
public class CheckUntaggedWaysAction extends AbstractAction {
    public CheckUntaggedWaysAction() { super(I18n.tr("Check: Untagged Ways")); }
    @Override public void actionPerformed(ActionEvent e) {
        DataSet ds = MainApplication.getLayerManager().getEditDataSet();
        if (ds == null) { JOptionPane.showMessageDialog(null, "No active OSM data layer found.", "MapathonQA", JOptionPane.WARNING_MESSAGE); return; }
        JDialog prog = CheckNonYesBuildingTagsAction.makeProgress("Checking untagged ways...");
        prog.setVisible(true);
        new SwingWorker<List<OsmPrimitive>, Void>() {
            @Override protected List<OsmPrimitive> doInBackground() { return runOn(ds, null, null); }
            @Override protected void done() {
                prog.dispose();
                try {
                    List<OsmPrimitive> f = get();
                    if (f.isEmpty()) { JOptionPane.showMessageDialog(null, "No untagged ways found.", "MapathonQA", JOptionPane.INFORMATION_MESSAGE); return; }
                    ds.setSelected(f);
                    JOptionPane.showMessageDialog(null, I18n.tr("{0} untagged way(s) found.\n\nThey are now selected. Untagged ways have no meaning\nin OSM and should be deleted or tagged appropriately.", f.size()), "MapathonQA \u2013 Untagged Ways", JOptionPane.WARNING_MESSAGE);
                } catch (Exception ex) { prog.dispose(); JOptionPane.showMessageDialog(null, "Check failed:\n"+ex.getMessage(), "MapathonQA", JOptionPane.ERROR_MESSAGE); }
            }
        }.execute();
    }
    public static List<OsmPrimitive> runOn(DataSet ds) { return runOn(ds, null, null); }
    public static List<OsmPrimitive> runOn(DataSet ds, java.util.Date since) { return runOn(ds, since, null); }
    public static List<OsmPrimitive> runOn(DataSet ds, java.util.Date since, java.util.Date until) {
        Set<Way> mp = new HashSet<>();
        for (Relation r : ds.getRelations()) { String t=r.get("type"); if ("multipolygon".equals(t)||"boundary".equals(t)) for (RelationMember rm : r.getMembers()) if (rm.isWay()) mp.add(rm.getWay()); }
        List<OsmPrimitive> f = new ArrayList<>();
        for (Way w : ds.getWays()) {
            if (w.isDeleted()||w.getNodesCount()<2||w.isIncomplete()) continue;
            boolean bad=false; for (Node n:w.getNodes()) { if (n==null||n.isIncomplete()||n.getCoor()==null) { bad=true; break; } } if (bad) continue;
            if (!GeometryUtil.isMappedDuring(w,since,until)) continue;
            if (w.getKeys().isEmpty()&&!mp.contains(w)) f.add(w);
        }
        return f;
    }
}
