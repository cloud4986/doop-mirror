package org.clyze.doop.common.android;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.*;
import org.xml.sax.SAXException;

public class AndroidManifestXML implements AndroidManifest {
    private static final String MANIFEST = "AndroidManifest.xml";
    private File archive;
    private String applicationName, packageName;
    private Set<String> activities = new HashSet<>();
    private Set<String> providers  = new HashSet<>();
    private Set<String> receivers  = new HashSet<>();
    private Set<String> services   = new HashSet<>();

    public static AndroidManifestXML fromArchive(String archiveLocation) throws IOException, ParserConfigurationException, SAXException {
        File ar = new File(archiveLocation);
        return new AndroidManifestXML(getZipEntryInputStream(ar, MANIFEST), ar);
    }

    public static AndroidManifestXML fromDir(String dir) throws IOException, ParserConfigurationException, SAXException {
        File ar = new File(dir + File.separator + MANIFEST);
        return new AndroidManifestXML(new FileInputStream(ar), ar);
    }

    private AndroidManifestXML(InputStream is, File ar) throws IOException, ParserConfigurationException, SAXException {
	this.archive = ar;

	DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
	Document doc = dbf.newDocumentBuilder().parse(is);

        Element docElem = doc.getDocumentElement();
        if (!docElem.getNodeName().equals("manifest"))
            throw new RuntimeException("No <manifest> root: " + archive);

        // Initialize 'packageName' field.
        NamedNodeMap rootAttrs = docElem.getAttributes();
        if (rootAttrs != null) {
            Node n = rootAttrs.getNamedItem("package");
            if (n != null) {
                packageName = n.getNodeValue();
            }
        }

        // Initialize 'applicationName' field.
        NodeList l0 = docElem.getChildNodes();
        for (int i0 = 0; i0 < l0.getLength(); i0++) {
            Node appNode = l0.item(i0);
            if (appNode.getNodeName().equals("application")) {
                NamedNodeMap appAttrs = appNode.getAttributes();
                if (appAttrs != null) {
                    Node appNodeName = appAttrs.getNamedItem("android:name");
                    if (appNodeName != null) {
                        applicationName = appNodeName.getNodeValue();
                        System.out.println("Application found: " + applicationName);
                    }
                }
                // Initialize activities/providers/receivers/services.
                NodeList l1 = appNode.getChildNodes();
                for (int i1 = 0; i1 < l1.getLength(); i1++) {
                    Node appElem = l1.item(i1);
                    registerAppNode(appElem, appElem.getAttributes());
                }
            }
        }
    }

    private void registerAppNode(Node appElem, NamedNodeMap attrs) {
        if (attrs == null)
            return;
        Node n = attrs.getNamedItem("android:name");
        if (n == null)
            return;
        if (appElem.getNodeName().equals("activity")) {
            activities.add(n.getNodeValue());
        } else if (appElem.getNodeName().equals("provider")) {
            providers.add(n.getNodeValue());
        } else if (appElem.getNodeName().equals("receiver")) {
            receivers.add(n.getNodeValue());
        } else if (appElem.getNodeName().equals("service")) {
            services.add(n.getNodeValue());
        }
    }

    public String getApplicationName() { return applicationName; }
    public String getPackageName()     { return packageName;     }
    public Set<String> getServices()   { return services;        }
    public Set<String> getActivities() { return activities;      }
    public Set<String> getProviders()  { return providers;       }
    public Set<String> getReceivers()  { return receivers;       }

    private static InputStream getZipEntryInputStreamLayout(File ar, String entry) {
        try {
            return getZipEntryInputStream(ar, entry);
        } catch (Exception ex) {
            final String[] altLayouts = { "v11", "v16", "v17", "v21", "v22" };
            for (String v : altLayouts ) {
                String l = entry.replaceAll("res/layout/", "res/layout-"+v+"/");
                try {
                    return getZipEntryInputStream(ar, l);
                } catch (Exception ex0) { }
            }
        }
        throw new RuntimeException("Cannot find layout " + entry);
    }

    private static InputStream getZipEntryInputStream(File ar, String entry) throws IOException {
        ZipInputStream zin = new ZipInputStream(new FileInputStream(ar));
        for (ZipEntry e; (e = zin.getNextEntry()) != null;) {
            if (e.getName().equals(entry) || e.getName().equals(entry + ".xml")) {
                return zin;
            }
        }
        throw new RuntimeException("Cannot find " + entry);
    }

    private void findOnClickHandlers(Node e, Set<String> accumulator) {
        NamedNodeMap attrs = e.getAttributes();
        if (attrs != null) {
            Node n = attrs.getNamedItem("android:onClick");
            if (n != null)
                accumulator.add(n.getNodeValue());
        }
        NodeList children = e.getChildNodes();
        for (int i = 0; i < children.getLength(); i++)
            findOnClickHandlers(children.item(i), accumulator);
    }

    public Set<String> getCallbackMethods() throws IOException {
        // We assume all callback methods are defined as
        // 'android:onClick' attributes in XML files under /res.

        Set<String> ret = new HashSet<>();
        ZipInputStream zin = null;
        try {
            zin = new ZipInputStream(new FileInputStream(archive));
        } catch (Exception ex) {
            ex.printStackTrace();
            return ret;
        }

        // Find all xml files under /res in the archive.
        Set<String> resXMLs = new HashSet<>();
        for (ZipEntry e; (e = zin.getNextEntry()) != null;) {
            String name = e.getName();
            if (name.startsWith("res/") &&
                (name.endsWith(".xml") || name.endsWith(".XML")))
                resXMLs.add(name);
        }

        // Parse each XML to find possible callbacks.
        try {
            for (String resXML : resXMLs) {
                DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
                InputStream is = getZipEntryInputStream(archive, resXML);
                Document doc = dbf.newDocumentBuilder().parse(is);
                findOnClickHandlers(doc.getDocumentElement(), ret);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return ret;
    }

    public Set<LayoutControl> getUserControls() {
        Set<String> layoutFiles = new HashSet<>();
        Set<LayoutControl> controls = new HashSet<>();
        try {
            ZipInputStream zin = new ZipInputStream(new FileInputStream(archive));
            for (ZipEntry e; (e = zin.getNextEntry()) != null;) {
                String name = e.getName();
                if (name.startsWith("res/layout") && name.endsWith(".xml"))
                    layoutFiles.add(name);
            }
            for (String layoutFile : layoutFiles)
                getUserControlsForLayoutFile(layoutFile, -1, controls);
        } catch (Exception ex) {
            System.err.println("Error while reading user controls:");
            ex.printStackTrace();
        }
        return controls;
    }

    void getUserControlsForLayoutFile(String layoutFile, int parentId,
                                      Set<LayoutControl> controls) throws Exception {
        InputStream is = getZipEntryInputStreamLayout(archive, layoutFile);
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        Document doc = dbf.newDocumentBuilder().parse(is);
        Element docElem = doc.getDocumentElement();
        NodeList l0 = docElem.getChildNodes();
        for (int i0 = 0; i0 < l0.getLength(); i0++) {
            getUserControlsForNode(l0.item(i0), parentId, controls);
        }
    }

    // Layout controls are triplets (id, control name, parent id) and
    // can be sensitive.
    private void getUserControlsForNode(Node node, int parentId,
                                        Set<LayoutControl> controls) throws Exception {
        String name = node.getNodeName();
        if (name.equals("dummy") || name.equals("#comment") || name.equals("#text")) {
            return;
        } else if (name.equals("include")) {
            String includedLayout = attrOrDefault(node, "layout", null);
            if (includedLayout != null) {
                if (includedLayout.startsWith("@layout/")) {
                    String layoutFile = "res/" + includedLayout.substring(1);
                    getUserControlsForLayoutFile(layoutFile, parentId, controls);
                } else {
                    System.err.println("unsupported include: " + includedLayout);
                }
            }
            return;
        }

        // Default control id, if no matching R entry is found.
        int intId = -1;
        // 'Merge' elements don't represent controls, skip to process children.
        if (!name.equals("merge")) {
            if (name.equals("fragment"))
                name = attrOrDefault(node, "android:name", "-1");
            String id = attrOrDefault(node, "android:id", "-1");
            if (id.startsWith("@+"))
                id = id.substring(2);
            if (id.indexOf("/") != -1) {
                String[] parts = id.split("/");
                Integer c = RLinker.getInstance().lookupConst(packageName, parts[0], parts[1]);
                if (c != null)
                    intId = c;
            }

            // Add a layout control with empty attributes.
            Map<String, Object> attrs = new HashMap<String, Object>();
            controls.add(new AndroidLayoutControl(intId, name, isSensitive(node), attrs, parentId));

            // Heuristic: if the name is unqualified, it comes from
            // android.view or android.widget ("Android Programming:
            // The Big Nerd Ranch Guide", chapter 32).
            if (name.lastIndexOf(".") == -1) {
                controls.add(new AndroidLayoutControl(intId, "android.view." + name, isSensitive(node), attrs, parentId));
                controls.add(new AndroidLayoutControl(intId, "android.widget." + name, isSensitive(node), attrs, parentId));
            }
        }

        NodeList l1 = node.getChildNodes();
        for (int i1 = 0; i1 < l1.getLength(); i1++)
            getUserControlsForNode(l1.item(i1), intId, controls);
    }

    private static boolean isSensitive(Node node) {
        String androidPassword = attrOrDefault(node, "android:password", null);
        if ((androidPassword != null) && androidPassword.equals("true"))
            return true;
        String inputT = attrOrDefault(node, "android:inputType", null);
        return (inputT != null) && (inputT.equals("textPassword") ||
                                    inputT.equals("textVisiblePassword") ||
                                    inputT.equals("textWebPassword") ||
                                    inputT.equals("numberPassword"));
    }

    // Read XML element attribute. On failure, return last default value.
    private static String attrOrDefault(Node node, String attr, String val) {
        NamedNodeMap attrs = node.getAttributes();
        if (attrs != null) {
            Node n = attrs.getNamedItem(attr);
            if (n != null)
                return n.getNodeValue();
        }
        return val;
    }

    private static class AndroidLayoutControl extends LayoutControl {
        private int id;
        private String viewClass;
        private boolean sensitive;
        private Map<String, Object> attrs;
        private int parentId;

        public AndroidLayoutControl(int id, String viewClass, boolean sensitive, Map<String, Object> attrs, int parentId) {
            this.id = id;
            this.viewClass = viewClass;
            this.sensitive = sensitive;
            this.attrs = attrs;
            this.parentId = parentId;
        }

        public int getID() { return id; }
        public boolean isSensitive() { return sensitive; }
        public String getViewClassName() { return viewClass; }
        public int getParentID() { return parentId; }
        public Map<String, Object> getAdditionalAttributes() { return attrs; }
    }
}
