package ome.services.blitz.test.utests;

import java.io.File;

import ome.services.blitz.repo.CheckedPath;
import ome.services.blitz.repo.path.ServerFilePathTransformer;
import ome.services.blitz.repo.path.MakePathComponentSafe;
import omero.ValidationException;
import omero.util.TempFileManager;

import org.apache.commons.io.FileUtils;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

@Test
public class CheckedPathTest {

    File dir;
    CheckedPath root;
    ServerFilePathTransformer serverPaths;

    @BeforeClass
    public void setup() throws Exception {
        this.dir = TempFileManager.create_path("repo", "test", true);
        this.serverPaths = new ServerFilePathTransformer();
        this.serverPaths.setBaseDirFile(this.dir);
        this.serverPaths.setPathSanitizer(new MakePathComponentSafe());
    }

    @Test
    public void testCtorWithRootPathPasses() throws Exception {
        CheckedPath cp = new CheckedPath(this.serverPaths, "", null);
        Assert.assertTrue(cp.isRoot);
    }

    @Test(expectedExceptions=ValidationException.class)
    public void testCtorWithPathAboveRootThrows() throws ValidationException {
        new CheckedPath(this.serverPaths, "..", null);
    }

    @Test
    public void testCtorWithPathBelowRootPasses() throws Exception {
        CheckedPath cp = new CheckedPath(this.serverPaths, "foo", null);
        Assert.assertFalse(cp.isRoot);
    }

    @Test
    public void testMustExistPassesWithExistingFile() throws Exception {
        File f = new File(this.dir, "foo");
        FileUtils.touch(f);
        CheckedPath cp = new CheckedPath(this.serverPaths, f.getName(), null);
        Assert.assertEquals(cp.mustExist(), cp);
    }

    @Test(expectedExceptions=ValidationException.class)
    public void testMustExistThrowsWithNonexistingFile()
            throws ValidationException {
        new CheckedPath(this.serverPaths, "bar", null).mustExist();
    }
}
