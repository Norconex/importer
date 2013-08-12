package com.norconex.importer.tagger.impl;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.configuration.XMLConfiguration;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import com.norconex.commons.lang.config.ConfigurationLoader;
import com.norconex.commons.lang.config.IXMLConfigurable;
import com.norconex.commons.lang.map.Properties;
import com.norconex.importer.tagger.IDocumentTagger;

/**
 * <p>Define and add values to documents found between a matching start and 
 * end strings.  The matching strings are defined in pairs and multiple ones 
 * can be specified at once.</p>
 * 
 * <p>This class can be used as a pre-parsing (text content-types only) 
 * or post-parsing handlers.</p>
 * <p>
 * XML configuration usage:
 * </p>
 * <pre>
 *  &lt;tagger class="com.norconex.importer.transformer.impl.TextBetweenTagger"
 *          inclusive="[false|true]" 
 *          caseSensitive="[false|true]" &gt;
 *      &lt;contentTypeRegex&gt;
 *          (regex to identify text content-types for pre-import, 
 *           overriding default)
 *      &lt;/contentTypeRegex&gt;
 *      &lt;restrictTo
 *              caseSensitive="[false|true]" &gt;
 *              property="(name of header/metadata name to match)"
 *          (regular expression of value to match)
 *      &lt;/restrictTo&gt;
 *      &lt;textBetween&gt
 *          &lt;start&gt(regex)&lt;/start&gt
 *          &lt;end&gt(regex)&lt;/end&gt
 *      &lt;/textBetween&gt
 *      &lt;-- multiple strignBetween tags allowed --&gt;
 *  &lt;/transformer&gt;
 * </pre>
 * @author Khalid AlHomoud
 * @author Pascal Essiembre
 */
public class TextBetweenTagger implements IDocumentTagger, IXMLConfigurable {

    private static final long serialVersionUID = 7202856816087407447L;

    private Set<Pair<String, String>> textPairs = 
            new TreeSet<Pair<String,String>>(
                    new Comparator<Pair<String,String>>() {
                        @Override
                        public int compare(Pair<String,String> o1, 
                                Pair<String,String> o2) {
                            return o1.getLeft().length() - o2.getLeft().length();
                        }
                    });

    private boolean inclusive;
    private boolean caseSensitive;
    private String name;


    @Override
    public void tagDocument(String reference, InputStream document,
            Properties metadata, boolean parsed) throws IOException {

        StringWriter writer = new StringWriter();
        IOUtils.copy(document, writer, "UTF-8");
        String docString = writer.toString();
        StringBuilder content = new StringBuilder(docString);

        int flags = Pattern.DOTALL | Pattern.UNICODE_CASE;
        if (!caseSensitive) {
            flags = flags | Pattern.CASE_INSENSITIVE;
        }
        for (Pair<String, String> pair : textPairs) {
            List<Pair<Integer, Integer>> matches = 
                    new ArrayList<Pair<Integer, Integer>>();
            Pattern leftPattern = Pattern.compile(pair.getLeft(), flags);
            Matcher leftMatch = leftPattern.matcher(content);
            while (leftMatch.find()) {
                Pattern rightPattern = Pattern.compile(pair.getRight(), flags);
                Matcher rightMatch = rightPattern.matcher(content);
                if (rightMatch.find(leftMatch.end())) {
                    if (inclusive) {
                        matches.add(new ImmutablePair<Integer, Integer>(
                                leftMatch.start(), rightMatch.end()));
                    } else {
                        matches.add(new ImmutablePair<Integer, Integer>(
                                leftMatch.end(), rightMatch.start()));
                    }
                } else {
                    break;
                }
            }
            for (int i = matches.size() -1; i >= 0; i--) {
                String value;
                Pair<Integer, Integer> matchPair = matches.get(i);
                value = content.substring(matchPair.getLeft(), matchPair.getRight());

                if (value.toString() != null) {
                    metadata.addString(name, value);

                }
            }
        }


    }
    
    public boolean isInclusive() {
        return inclusive;
    }
    /**
     * Sets whether start and end text pairs should themselves be stripped or 
     * not.
     * @param inclusive <code>true</code> to strip start and end text
     */
    public void setInclusive(boolean inclusive) {
        this.inclusive = inclusive;
    }
    public boolean isCaseSensitive() {
        return caseSensitive;
    }
    /**
     * Sets whether to ignore case when matching start and end text.
     * @param caseSensitive <code>true</code> to consider character case
     */
    public void setCaseSensitive(boolean caseSensitive) {
        this.caseSensitive = caseSensitive;
    }
    public void addTextEndpoints(String fromText, String toText) {
        if (StringUtils.isBlank(fromText) || StringUtils.isBlank(toText)) {
            return;
        }
        textPairs.add(new ImmutablePair<String, String>(fromText, toText));
    }
    @Override
    public void loadFromXML(Reader in) throws IOException {
        XMLConfiguration xml = ConfigurationLoader.loadXML(in);
        setCaseSensitive(xml.getBoolean("[@caseSensitive]", false));
        setInclusive(xml.getBoolean("[@inclusive]", false));
        List<HierarchicalConfiguration> nodes = 
                xml.configurationsAt("textBetween");
        for (HierarchicalConfiguration node : nodes) {
            name = node.getString("[@name]");
            addTextEndpoints(
                    node.getString("start", null), node.getString("end", null));
        }
    }

    @Override
    public void saveToXML(Writer out) throws IOException {
        XMLOutputFactory factory = XMLOutputFactory.newInstance();
        try {
            XMLStreamWriter writer = factory.createXMLStreamWriter(out);
            writer.writeStartElement("tagger");
            writer.writeAttribute("class", getClass().getCanonicalName());
            writer.writeAttribute(
                    "caseSensitive", Boolean.toString(isCaseSensitive()));
            writer.writeAttribute("inclusive", Boolean.toString(isInclusive()));
            for (Pair<String, String> pair : textPairs) {
                writer.writeStartElement("textBetween");
                writer.writeAttribute("name", name);
                writer.writeStartElement("start");
                writer.writeCharacters(pair.getLeft());
                writer.writeEndElement();
                writer.writeStartElement("end");
                writer.writeCharacters(pair.getRight());
                writer.writeEndElement();
                writer.writeEndElement();
            }
            writer.writeEndElement();
            writer.flush();
            writer.close();
        } catch (XMLStreamException e) {
            throw new IOException("Cannot save as XML.", e);
        }
    }

}
