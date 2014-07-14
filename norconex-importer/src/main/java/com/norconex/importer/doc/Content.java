/**
 * 
 */
package com.norconex.importer.doc;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

import org.apache.commons.io.IOUtils;
import org.apache.commons.io.input.NullInputStream;

import com.norconex.commons.lang.io.CachedInputStream;
import com.norconex.commons.lang.unit.DataUnit;

/**
 * This class is not thread-safe.
 * @author Pascal Essiembre
 */
public class Content {
    //TODO make this a unique representation of content, which can be 
    // a file, an inputstream and maybe others.
    // Provide utility methods too.
    
    public static final int DEFAULT_MAX_MEMORY_CACHE_SIZE = 
            (int) DataUnit.MB.toBytes(1);
    public static final Content NO_CONTENT = new Content(
            new NullInputStream(0), 0);
    
    private CachedInputStream cacheStream;
    
    public Content(File file) throws FileNotFoundException {
        this(new FileInputStream(file), DEFAULT_MAX_MEMORY_CACHE_SIZE);
    }
    public Content(File file, int maxMemoryCacheSize) 
            throws FileNotFoundException {
        this(new FileInputStream(file), maxMemoryCacheSize);
    }
    public Content(InputStream is) {
        this(is, DEFAULT_MAX_MEMORY_CACHE_SIZE);
    }
    public Content(InputStream is, int maxMemoryCacheSize) {
        super();
        if (is == null) {
            cacheStream = new CachedInputStream(new NullInputStream(0), 0);
        } else {
            cacheStream = new CachedInputStream(is, maxMemoryCacheSize);
        }
    }
    public Content(CachedInputStream is) {
        cacheStream = is;
    }
    
    /**
     * This method is not thread-safe.  
     * @return input stream
     */
    public CachedInputStream getInputStream() {
        return cacheStream;
    }
    
    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        if (cacheStream != null) {
            IOUtils.closeQuietly(cacheStream);
        }
    }
}
