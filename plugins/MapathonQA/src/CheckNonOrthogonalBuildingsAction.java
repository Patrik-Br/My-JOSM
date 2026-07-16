package org.openstreetmap.josm.plugins.mapathonqa;
import java.awt.BorderLayout; import java.awt.event.ActionEvent;
import java.util.*; import javax.swing.*;
import org.openstreetmap.josm.data.osm.*;
import org.openstreetmap.josm.gui.MainApplication; import org.openstreetmap.josm.tools.I18n;
public class CheckNonOrthogonalBuildingsAction extends AbstractAction {
    private static final double SQ_TH=1.0, RD_TH=1.0; private static final int MAX_N=18;
    public CheckNonOrthogonalBuildingsAction() { super(I18n.tr("Check: Non-orthogonal Buildings")); }
    @Override public void actionPerformed(ActionEvent e) {
        DataSet ds = MainApplication.getLayerManager().getEditDataSet();
        if (ds == null) { JOptionPane.showMessageDialog(null, "No active OSM data layer found.", "MapathonQA", JOptionPane.WARNING_MESSAGE); return; }
        JDialog prog = CheckNonYesBuildingTagsAction.makeProgress("Checking building orthogonality...");
        prog.setVisible(true);
        new SwingWorker<List<OsmPrimitive>, Void>() {
            @Override protected List<OsmPrimitive> doInBackground() { return runOn(ds, null, null); }
            @Override protected void done() {
                prog.dispose();
                try {
                    List<OsmPrimitive> f = get();
                    if (f.isEmpty()) { JOptionPane.showMessageDialog(null, "No non-orthogonal buildings found.", "MapathonQA", JOptionPane.INFORMATION_MESSAGE); return; }
                    ds.setSelected(f);
                    JOptionPane.showMessageDialog(null, I18n.tr("{0} building(s) have non-orthogonal corners.\n\nThey are now selected. Use the Orthogonalize Shape\ntool (Q key in JOSM) to fix the corners.", f.size()), "MapathonQA \u2013 Non-orthogonal Buildings", JOptionPane.WARNING_MESSAGE);
                } catch (Exception ex) { prog.dispose(); JOptionPane.showMessageDialog(null, "Check failed:\n"+ex.getMessage(), "MapathonQA", JOptionPane.ERROR_MESSAGE); }
            }
        }.execute();
    }
    public static List<OsmPrimitive> runOn(DataSet ds) { return runOn(ds, null, null); }
    public static List<OsmPrimitive> runOn(DataSet ds, java.util.Date since) { return runOn(ds, since, null); }
    public static List<OsmPrimitive> runOn(DataSet ds, java.util.Date since, java.util.Date until) {
        List<OsmPrimitive> f = new ArrayList<>();
        for (Way w : ds.getWays()) {
            if (!w.isClosed()||!w.hasKey("building")) continue;
            if (!GeometryUtil.isMappedDuring(w,since,until)) continue;
            int nc=w.getNodesCount()-1; if (nc<3||nc>=MAX_N) continue;
            if (classifyBuilding(w,nc,SQ_TH,RD_TH)==4) f.add(w);
        }
        return f;
    }
    static int classifyBuilding(Way w, int n, double sqTh, double rdTh) {
        List<Node> nodes=w.getNodes(); double ep=180.0-360.0/n;
        double clat=0; int valid=0;
        for (int i=0;i<n;i++) { Node nd=nodes.get(i); if (nd!=null&&nd.getCoor()!=null) { clat+=nd.lat(); valid++; } }
        double cos=valid>0?Math.cos(Math.toRadians(clat/valid)):1.0; if (cos<0.01) cos=0.01;
        double ssd=0; int iSq=0,iRd=0,nz=0,mSq=0,mRd=0; final double M=15.0;
        for (int i=0;i<n;i++) {
            Node p=nodes.get((i-1+n)%n),c=nodes.get(i),nx=nodes.get((i+1)%n);
            if (p==null||c==null||nx==null||p.getCoor()==null||c.getCoor()==null||nx.getCoor()==null) continue;
            double ax=(p.lon()-c.lon())*cos,ay=p.lat()-c.lat(),bx=(nx.lon()-c.lon())*cos,by=nx.lat()-c.lat();
            double la=Math.sqrt(ax*ax+ay*ay),lb=Math.sqrt(bx*bx+by*by); if (la<1e-10||lb<1e-10) continue;
            double dot=Math.max(-1.0,Math.min(1.0,(ax*bx+ay*by)/(la*lb)));
            double ang=Math.toDegrees(Math.acos(dot));
            double sd=Math.min(Math.abs(90.0-ang),Math.abs(180.0-ang)); ssd+=sd;
            if (sd<sqTh) iSq++; else if (sd<M) mSq++;
            double rd=Math.abs(ep-ang); if (rd<rdTh) iRd++; else if (rd<M) mRd++;
            if (Math.abs(ang)<sqTh) nz++;
        }
        ssd=ssd%90.0;
        if (iSq==n) return 2; if (iRd==n&&n>4) return 1;
        if (iRd+mRd>iSq+mSq) return mRd>0?3:4;
        if (mSq>0) return 4; if (ssd<sqTh&&nz==0) return 2; return 4;
    }
}
