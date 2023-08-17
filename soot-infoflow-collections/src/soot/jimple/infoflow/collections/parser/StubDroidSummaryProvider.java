package soot.jimple.infoflow.collections.parser;

import soot.jimple.infoflow.methodSummary.data.provider.XMLSummaryProvider;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public class StubDroidSummaryProvider extends XMLSummaryProvider {

    /**
     * Loads a summary from a folder within the StubDroid jar file.
     *
     * @param folderInJar The folder in the JAR file from which to load the summary
     *                    files
     * @throws URISyntaxException
     * @throws IOException
     */
    public StubDroidSummaryProvider(String folderInJar) throws URISyntaxException, IOException {
        this(folderInJar, StubDroidSummaryProvider.class);
    }

    /**
     * Loads a summary from a folder within the StubDroid jar file.
     *
     * @param folderInJar The folder in the JAR file from which to load the summary
     *                    files
     * @param parentClass The class in whose jar to look for the summary files
     * @throws URISyntaxException
     * @throws IOException
     */
    public StubDroidSummaryProvider(String folderInJar, Class<?> parentClass) throws URISyntaxException, IOException {
        summaryReader = new StubDroidParser();
        loadSummariesFromJAR(folderInJar, parentClass, p -> loadClass(p));
    }

    /**
     * Loads a file or all files in a dir (not recursively)
     *
     * @param source The single file or directory to load
     */
    public StubDroidSummaryProvider(File source) {
        this(Collections.singletonList(source));
    }

    /**
     * Loads the summaries from all of the given files
     *
     * @param files The files to load
     */
    public StubDroidSummaryProvider(List<File> files) {
        summaryReader = new StubDroidParser();
        loadSummariesFromFiles(files, f -> loadClass(f));
    }

    @Override
    public boolean mayHaveSummaryForMethod(String subsig) {
        return subsigMethodsWithSummaries.contains(subsig);
    }

    @Override
    public Set<String> getAllClassesWithSummaries() {
        return loadedClasses;
    }

}
