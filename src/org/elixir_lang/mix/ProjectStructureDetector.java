package org.elixir_lang.mix;

import com.intellij.ide.projectView.actions.MarkRootActionBase;
import com.intellij.ide.util.importProject.LibraryDescriptor;
import com.intellij.ide.util.importProject.ModuleDescriptor;
import com.intellij.ide.util.importProject.ProjectDescriptor;
import com.intellij.ide.util.projectWizard.ModuleWizardStep;
import com.intellij.ide.util.projectWizard.ProjectJdkForModuleStep;
import com.intellij.ide.util.projectWizard.importSources.ProjectFromSourcesBuilder;
import com.intellij.ide.util.projectWizard.importSources.impl.ProjectFromSourcesBuilderImpl;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.*;
import com.intellij.openapi.roots.impl.DirectoryIndex;
import com.intellij.openapi.roots.libraries.Library;
import com.intellij.openapi.roots.libraries.LibraryTable;
import com.intellij.openapi.roots.libraries.LibraryTablesRegistrar;
import com.intellij.openapi.roots.ui.configuration.ModulesProvider;
import com.intellij.openapi.util.Comparing;
import com.intellij.openapi.vfs.VirtualFile;
import org.elixir_lang.SdkType;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class ProjectStructureDetector extends com.intellij.ide.util.projectWizard.importSources.ProjectStructureDetector implements ProjectFromSourcesBuilderImpl.ProjectConfigurationUpdater {
    @NotNull
    @Override
    public DirectoryProcessingResult detectRoots(@NotNull File dir, @NotNull File[] children, @NotNull File base, @NotNull List<com.intellij.ide.util.projectWizard.importSources.DetectedProjectRoot> result) {
        DirectoryProcessingResult directoryProcessingResult = DirectoryProcessingResult.PROCESS_CHILDREN;

        for (File child : children) {
            String childName = child.getName();

            if (childName.equals("mix.exs") && child.isFile()) {
                result.add(new DetectedProjectRoot(dir));
                directoryProcessingResult = DirectoryProcessingResult.SKIP_CHILDREN;

                break;
            }
        }

        return directoryProcessingResult;
    }

    /**
     * @link package org.intellij.erlang.editor.ErlangProjectStructureDetector#createWizardSteps
     */
    @Override
    public List<ModuleWizardStep> createWizardSteps(ProjectFromSourcesBuilder builder, ProjectDescriptor projectDescriptor, Icon stepIcon) {
        /* ProjectJdkForModuleStep should actually be called ProjectSdkForModuleStep since it uses the Sdk type, and
           doesn't assume a Jdk. */
        ProjectJdkForModuleStep projectJdkForModuleStep = new ProjectJdkForModuleStep(builder.getContext(), SdkType.getInstance());
        return Collections.<ModuleWizardStep>singletonList(projectJdkForModuleStep);
    }

    /**
     *
     * @param roots
     * @param projectDescriptor
     * @param builder
     * @see  org.intellij.erlang.editor.ErlangProjectStructureDetector.setupProjectStructure
     */
    @Override
    public void setupProjectStructure(@NotNull Collection<com.intellij.ide.util.projectWizard.importSources.DetectedProjectRoot> roots, @NotNull ProjectDescriptor projectDescriptor, @NotNull ProjectFromSourcesBuilder builder) {
        ProjectFromSourcesBuilderImpl builderImpl = (ProjectFromSourcesBuilderImpl) builder;
        builderImpl.addConfigurationUpdater(this);

        if (!roots.isEmpty() && !builder.hasRootsFromOtherDetectors(this)) {
            List<LibraryDescriptor> libraries = new ArrayList<LibraryDescriptor>();
            List<ModuleDescriptor> modules = projectDescriptor.getModules();

            if (modules.isEmpty()) {
                modules = new ArrayList<ModuleDescriptor>(roots.size());

                for (com.intellij.ide.util.projectWizard.importSources.DetectedProjectRoot root : roots) {
                    File rootDirectory = root.getDirectory();
                    Collection<DetectedSourceRoot> sourceRoots = new ArrayList<DetectedSourceRoot>(2);

                    File lib = new File(rootDirectory, "lib");

                    if (lib.isDirectory()) {
                        sourceRoots.add(new DetectedSourceRoot(lib));
                    }

                    File test = new File(rootDirectory, "test");

                    if (test.isDirectory()) {
                        sourceRoots.add(new DetectedSourceRoot(test));
                    }

                    modules.add(
                            new ModuleDescriptor(
                                    rootDirectory,
                                    ModuleType.getInstance(),
                                    sourceRoots
                            )
                    );

                    File deps = new File(rootDirectory, "deps");

                    if (deps.isDirectory()) {
                        for (File depChild : deps.listFiles()) {
                            if (depChild.isDirectory()) {
                                libraries.add(
                                        new LibraryDescriptor(
                                                depChild.getName(),
                                                Collections.singletonList(depChild)
                                        )
                                );
                            }
                        }
                    }
                }

                projectDescriptor.setLibraries(libraries);
                projectDescriptor.setModules(modules);
            }
        }
    }

    /**
     *
     * @param project
     * @param modelsProvider
     * @param modulesProvider
     * @see com.intellij.ide.projectView.actions.MarkRootActionBase#actionPerformed(AnActionEvent)
     * @see com.intellij.ide.projectView.actions.MarkRootActionBase#findContentEntry(ModuleRootModel, VirtualFile)
     * @see com.intellij.ide.projectView.actions.MarkRootActionBase#findParentModule
     * @see com.intellij.ide.projectView.actions.MarkRootActionBase.modifyRoots
     */
    @Override
    public void updateProject(@NotNull final Project project, @NotNull ModifiableModelsProvider modelsProvider, @NotNull ModulesProvider modulesProvider) {
        updateProjectLibraries(project);
        excludeOutputDirectories(project, modelsProvider);
    }

    /*
     * Private
     */

    private static void excludeDirectory(@NotNull final Project project, @NotNull ModifiableModelsProvider modelsProvider, DirectoryIndex directoryIndex, VirtualFile child) {
        Module module = directoryIndex.getInfoForFile(child).getModule();
        final ModifiableRootModel moduleModel = modelsProvider.getModuleModifiableModel(module);

        ContentEntry contentEntry = MarkRootActionBase.findContentEntry(moduleModel, child);

        if (contentEntry != null) {
            excludeDirectoryFromContentEntry(project, child, moduleModel, contentEntry);
        }
    }

    private static void excludeDirectoryFromContentEntry(@NotNull final Project project, VirtualFile child, final ModifiableRootModel moduleModel, ContentEntry contentEntry) {
        unmarkDirectoryAsSoureFromContentEntry(child, contentEntry);

        contentEntry.addExcludeFolder(child);

        ApplicationManager.getApplication().runWriteAction(
                new Runnable() {
                    @Override
                    public void run() {
                        moduleModel.commit();
                        project.save();
                    }
                }
        );
    }

    private static void excludeOutputDirectories(@NotNull final Project project, @NotNull ModifiableModelsProvider modelsProvider) {
        VirtualFile baseDir = project.getBaseDir();
        DirectoryIndex directoryIndex = DirectoryIndex.getInstance(project);

        for (VirtualFile child : baseDir.getChildren()) {
            String childName = child.getName();

            // _build is for mix
            // rel is for exrm
            if (childName.equals("_build") || childName.equals("rel")) {
                excludeDirectory(project, modelsProvider, directoryIndex, child);
            }
        }
    }

    private static void unmarkDirectoryAsSoureFromContentEntry(VirtualFile child, ContentEntry contentEntry) {
        final SourceFolder[] sourceFolders = contentEntry.getSourceFolders();

        for (SourceFolder sourceFolder : sourceFolders) {
            if(Comparing.equal(sourceFolder.getFile(), child)) {
                contentEntry.removeSourceFolder(sourceFolder);
            }

            break;
        }
    }

    private void updateProjectLibraries(@NotNull Project project) {
        LibraryTable libraryTable = LibraryTablesRegistrar.getInstance().getLibraryTable(project);

        for (Library library : libraryTable.getLibraries()) {
            Library.ModifiableModel libraryModel = library.getModifiableModel();

            // LibraryDescriptor 'jars' are marked as OrderRootType.CLASSES in NewProjectUtil.createFromWizard
            VirtualFile[] rootVirtualFiles = libraryModel.getFiles(OrderRootType.CLASSES);

            for (VirtualFile rootVirtualFile : rootVirtualFiles) {
                // Remove the default assigned by NewProjectUtil.createFromWizard
                libraryModel.removeRoot(rootVirtualFile.getUrl(), OrderRootType.CLASSES);

                for (VirtualFile childVirtualFile : rootVirtualFile.getChildren()) {
                    // mix project sources
                    if (childVirtualFile.getName().equals("lib") && childVirtualFile.isDirectory()) {
                        libraryModel.addRoot(childVirtualFile, OrderRootType.SOURCES);
                    }

                    // erlang sources
                    if (childVirtualFile.getName().equals("src") && childVirtualFile.isDirectory()) {
                        libraryModel.addRoot(childVirtualFile, OrderRootType.SOURCES);
                    }
                }
            }

            libraryModel.commit();
        }
    }
}
