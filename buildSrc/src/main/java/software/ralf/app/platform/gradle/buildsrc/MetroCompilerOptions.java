package software.ralf.app.platform.gradle.buildsrc;

import java.util.ArrayList;
import java.util.List;
import kotlin.Unit;
import org.gradle.api.Project;
import org.jetbrains.kotlin.gradle.plugin.CompilerPluginConfig;
import org.jetbrains.kotlin.gradle.plugin.KotlinPluginLifecycle;
import org.jetbrains.kotlin.gradle.plugin.KotlinPluginLifecycleKt;
import org.jetbrains.kotlin.gradle.plugin.SubpluginOption;
import org.jetbrains.kotlin.gradle.tasks.AbstractKotlinCompile;
import org.jetbrains.kotlin.gradle.tasks.CompilerPluginOptions;
import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile;
import org.jetbrains.kotlin.gradle.tasks.KotlinNativeCompile;

final class MetroCompilerOptions {
  private static final String APP_PLATFORM_COMPILER_PLUGIN_ID =
      "software.ralf.app.platform.metro.compiler";
  private static final String METRO_COMPILER_PLUGIN_ID = "dev.zacsweers.metro.compiler";
  private static final String GENERATE_CLASSES_IN_IR = "generate-classes-in-ir";
  private static final String GENERATE_CONTRIBUTION_HINTS_IN_FIR =
      "generate-contribution-hints-in-fir";

  private MetroCompilerOptions() {}

  /**
   * Mirrors Metro's class-generation mode and keeps KLIB package-discovery hints in FIR.
   *
   * <p>App Platform uses the mirrored mode to choose between its IR declaration generator and FIR
   * fallback. Kotlin cannot discover package-level callables generated in IR from a downstream
   * KLIB compilation, so non-JVM compilations retain FIR hints without changing Metro's class
   * generation mode.
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
    List<CompilerPluginConfig> pluginOptions = task.getPluginOptions().get();
    boolean generateClassesInIr =
        pluginOptions.stream()
            .flatMap(
                options ->
                    options
                        .allOptions()
                        .getOrDefault(METRO_COMPILER_PLUGIN_ID, List.of())
                        .stream())
            .filter(option -> GENERATE_CLASSES_IN_IR.equals(option.getKey()))
            .findFirst()
            .map(SubpluginOption::getValue)
            .map(Boolean::parseBoolean)
            .orElse(true);
    List<CompilerPluginOptions> updatedOptions = new ArrayList<>();
    pluginOptions.forEach(
        options ->
            updatedOptions.add(
                withAppPlatformOptions(options, !(task instanceof KotlinJvmCompile))));
    if (updatedOptions.isEmpty()) {
      updatedOptions.add(new CompilerPluginOptions());
    }
    updatedOptions
        .get(0)
        .addPluginArgument(
            APP_PLATFORM_COMPILER_PLUGIN_ID,
            new SubpluginOption(GENERATE_CLASSES_IN_IR, Boolean.toString(generateClassesInIr)));
    task.getPluginOptions().set(updatedOptions);
  }

  private static void replaceMetroFirHints(KotlinNativeCompile task) {
    boolean generateClassesInIr = generateClassesInIr(task.getCompilerPluginOptions());
    CompilerPluginOptions updatedOptions =
        withAppPlatformOptions(task.getCompilerPluginOptions(), true);
    updatedOptions.addPluginArgument(
        APP_PLATFORM_COMPILER_PLUGIN_ID,
        new SubpluginOption(GENERATE_CLASSES_IN_IR, Boolean.toString(generateClassesInIr)));
    task.getCompilerPluginOptions().allOptions().clear();
    updatedOptions
        .allOptions()
        .forEach(
            (pluginId, options) ->
                options.forEach(
                    option ->
                        task.getCompilerPluginOptions().addPluginArgument(pluginId, option)));
  }

  private static CompilerPluginOptions withAppPlatformOptions(
      CompilerPluginConfig pluginOptions, boolean generateFirHints) {
    CompilerPluginOptions updatedOptions = new CompilerPluginOptions();
    pluginOptions
        .allOptions()
        .forEach(
            (pluginId, options) ->
                options.forEach(
                    option -> {
                      if (APP_PLATFORM_COMPILER_PLUGIN_ID.equals(pluginId)
                          && GENERATE_CLASSES_IN_IR.equals(option.getKey())) {
                        return;
                      }
                      updatedOptions.addPluginArgument(
                          pluginId, withMetroFirHints(pluginId, option, generateFirHints));
                    }));
    return updatedOptions;
  }

  private static boolean generateClassesInIr(CompilerPluginConfig pluginOptions) {
    return pluginOptions.allOptions().getOrDefault(METRO_COMPILER_PLUGIN_ID, List.of()).stream()
        .filter(option -> GENERATE_CLASSES_IN_IR.equals(option.getKey()))
        .findFirst()
        .map(SubpluginOption::getValue)
        .map(Boolean::parseBoolean)
        .orElse(true);
  }

  private static SubpluginOption withMetroFirHints(
      String pluginId, SubpluginOption option, boolean generateFirHints) {
    if (generateFirHints
        && METRO_COMPILER_PLUGIN_ID.equals(pluginId)
        && GENERATE_CONTRIBUTION_HINTS_IN_FIR.equals(option.getKey())) {
      return new SubpluginOption(option.getKey(), "true");
    }
    return option;
  }
}
