package de.greensurvivors.headnseek.paper.startup;

import io.papermc.paper.plugin.loader.PluginClasspathBuilder;
import io.papermc.paper.plugin.loader.PluginLoader;
import io.papermc.paper.plugin.loader.library.impl.MavenLibraryResolver;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.repository.RemoteRepository;
import org.jetbrains.annotations.NotNull;

@SuppressWarnings({"UnstableApiUsage", "unused"}) // it IS used, just by paper not we ourselves
public class Loader implements PluginLoader {
    @Override
    public void classloader(final @NotNull PluginClasspathBuilder classpathBuilder) {
        MavenLibraryResolver resolver = new MavenLibraryResolver();
        // todo: [EntrypointUtil] Class io.papermc.paper.plugin.loader.library.impl.MavenLibraryResolver does not have member field 'java.lang.String MAVEN_CENTRAL_DEFAULT_MIRROR'
        resolver.addRepository(new RemoteRepository.Builder("central", "default", "https://maven-central.storage-download.googleapis.com/maven2" /*MavenLibraryResolver.MAVEN_CENTRAL_DEFAULT_MIRROR*/).build());

        resolver.addDependency(new Dependency(new DefaultArtifact("com.github.ben-manes.caffeine:caffeine:${caffeine_version}"), null));
        classpathBuilder.addLibrary(resolver);
    }
}
