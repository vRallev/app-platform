package software.ralf.app.platform.gradle.buildsrc;

import java.util.List;
import kotlin.Unit;
import org.gradle.api.Project;
import org.jetbrains.kotlin.gradle.plugin.CompilerPluginConfig;
import org.jetbrains.kotlin.gradle.plugin.KotlinPluginLifecycle;
import org.jetbrains.kotlin.gradle.plugin.KotlinPluginLifecycleKt;
import org.jetbrains.kotlin.gradle.plugin.SubpluginOption;
import org.jetbrains.kotlin.gradle.tasks.AbstractKotlinCompile;
import org.jetbrains.kotlin.gradle.tasks.CompilerPluginOptions;
import org.jetbrains.kotlin.gradle.tasks.KotlinNativeCompile;

final class MetroCompilerOptions {
  private static final String METRO_COMPILER_PLUGIN_ID = "dev.zacsweers.metro.compiler";
  private static final String GENERATE_CONTRIBUTION_HINTS_IN_FIR =
      "generate-contribution-hints-in-fir";

  private MetroCompilerOptions() {}

  /**
   * Keeps Metro's package-discovery hints in FIR while all generated classes remain in IR.
   *
   * <p>Metro 1.4.0 couples these two settings in its Gradle plugin even though Kotlin cannot
   * discover package-level callables generated in IR from a downstream KLIB compilation. This
   * replaces only the hint option after Metro has configured each task, leaving
   * {@code generate-classes-in-ir} enabled.
   */
  @SuppressWarnings({"rawtypes", "unchecked"})
  static void enable(Project project) {
    KotlinPluginLifecycleKt.launchInStage(
        project,
        KotlinPluginLifecycle.Stage.AfterFinaliseCompilations,
        (lifecycle, continuation) -> {
          project
              .getTasks()
              .withType(AbstractKotlinCompile.class)
              .configureEach(MetroCompilerOptions::replaceMetroFirHints);
          project
              .getTasks()
              .withType(KotlinNativeCompile.class)
              .configureEach(MetroCompilerOptions::replaceMetroFirHints);
          return Unit.INSTANCE;
        });
  }

  private static void replaceMetroFirHints(AbstractKotlinCompile<?> task) {
    List<CompilerPluginOptions> updatedOptions =
        task.getPluginOptions().get().stream()
            .map(MetroCompilerOptions::withMetroFirHints)
            .toList();
    task.getPluginOptions().set(updatedOptions);
  }

  private static void replaceMetroFirHints(KotlinNativeCompile task) {
    CompilerPluginOptions updatedOptions = withMetroFirHints(task.getCompilerPluginOptions());
    task.getCompilerPluginOptions().allOptions().clear();
    updatedOptions
        .allOptions()
        .forEach(
            (pluginId, options) ->
                options.forEach(
                    option ->
                        task.getCompilerPluginOptions().addPluginArgument(pluginId, option)));
  }

  private static CompilerPluginOptions withMetroFirHints(CompilerPluginConfig pluginOptions) {
    CompilerPluginOptions updatedOptions = new CompilerPluginOptions();
    pluginOptions
        .allOptions()
        .forEach(
            (pluginId, options) ->
                options.forEach(
                    option ->
                        updatedOptions.addPluginArgument(
                            pluginId, withMetroFirHints(pluginId, option))));
    return updatedOptions;
  }

  private static SubpluginOption withMetroFirHints(String pluginId, SubpluginOption option) {
    if (METRO_COMPILER_PLUGIN_ID.equals(pluginId)
        && GENERATE_CONTRIBUTION_HINTS_IN_FIR.equals(option.getKey())) {
      return new SubpluginOption(option.getKey(), "true");
    }
    return option;
  }
}
