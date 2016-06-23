package com.norconex.importer.parser;

/**
 * Indicates that a parser can be initialized with generic parser configuration
 * settings and it will try to apply any such settings the best it can
 * when possible to do so.  Those settings are typically general settings
 * that applies to more than one parser and can thus be configured "globally"
 * for all applicable parsers.
 * It should be left to {@link IDocumentParserFactory} implementations
 * to initialize parsers as they see fit.
 * The default {@link GenericDocumentParserFactory} will always invoke the
 * {@link #initialize(ParseHints)} method at least once per configured parsers. 
 * 
 * @author Pascal Essiembre
 * @since 2.6.0
 */
public interface IHintsAwareParser extends IDocumentParser {

    /**
     * Initialize this parser with the given parse hints.  While not mandatory,
     * aware parsers are strongly encouraged to support applicable hints.
     * @param parserHints configuration settings influencing parsing when 
     * possible or appropriate
     */
    void initialize(ParseHints parserHints);
    
}
