package ee.carlrobert.codegpt.settings.service.llama;

import static ee.carlrobert.codegpt.credentials.CredentialsStore.CredentialKey.LLAMA_API_KEY;
import static ee.carlrobert.codegpt.settings.service.ServiceType.LLAMA_CPP;
import static org.apache.commons.lang3.SystemUtils.IS_OS_LINUX;
import static org.apache.commons.lang3.SystemUtils.IS_OS_MAC_OSX;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import ee.carlrobert.codegpt.codecompletions.InfillPromptTemplate;
import ee.carlrobert.codegpt.completions.HuggingFaceModel;
import ee.carlrobert.codegpt.completions.llama.LlamaModel;
import ee.carlrobert.codegpt.credentials.CredentialsStore;
import ee.carlrobert.codegpt.settings.GeneralSettings;
import ee.carlrobert.codegpt.settings.service.llama.form.LlamaSettingsForm;
import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

@State(name = "CodeGPT", storages = @Storage("CodeGPT_LlamaSettings.xml"))
public class LlamaSettings implements PersistentStateComponent<LlamaSettingsState> {

  private LlamaSettingsState state = new LlamaSettingsState();

  @Override
  @NotNull
  public LlamaSettingsState getState() {
    return state;
  }

  @Override
  public void loadState(@NotNull LlamaSettingsState state) {
    this.state = state;
    // Catch if model's name has changed which could lead to
    // HuggingFaceModel or PromptTemplates not being found
    if (this.state.getHuggingFaceModel() == null) {
      this.state.setHuggingFaceModel(HuggingFaceModel.CODE_LLAMA_7B_Q4);
    }
    if (this.state.getRemoteModelInfillPromptTemplate() == null) {
      this.state.setRemoteModelInfillPromptTemplate(InfillPromptTemplate.CODE_LLAMA);
    }
    if (this.state.getLocalModelPromptTemplate() == null) {
      this.state.setLocalModelInfillPromptTemplate(InfillPromptTemplate.CODE_LLAMA);
    }
  }

  public static LlamaSettingsState getCurrentState() {
    return getInstance().getState();
  }

  /**
   * Code Completions enabled in settings and a model with InfillPromptTemplate selected.
   */
  public static boolean isCodeCompletionsPossible() {
    return getInstance().getState().isCodeCompletionsEnabled()
            && LlamaModel.findByHuggingFaceModel(getInstance().getState().getHuggingFaceModel())
                    .getInfillPromptTemplate() != null;
  }

  public static LlamaSettings getInstance() {
    return ApplicationManager.getApplication().getService(LlamaSettings.class);
  }

  public boolean isModified(LlamaSettingsForm form) {
    return !form.getCurrentState().equals(state)
        || !StringUtils.equals(
        form.getLlamaServerPreferencesForm().getApiKey(),
        CredentialsStore.getCredential(LLAMA_API_KEY));
  }

  public static boolean isRunnable() {
    return (IS_OS_MAC_OSX || IS_OS_LINUX)
            && GeneralSettings.getCurrentState().getSelectedService() == LLAMA_CPP;
  }

  public static boolean isRunnable(HuggingFaceModel model) {
    return isRunnable() && isModelExists(model);
  }

  public static boolean isModelExists(HuggingFaceModel model) {
    return getLlamaModelsPath().resolve(model.getFileName()).toFile().exists();
  }

  public static Path getLlamaModelsPath() {
    return Paths.get(System.getProperty("user.home"), ".codegpt/models/gguf");
  }

  // Copied from LlamaModelPreferencesForm
  public String getActualModelPath() {
    return state.isUseCustomModel()
            ? state.getCustomLlamaModelPath()
            : getLlamaModelsPath() + File.separator
            + state.getHuggingFaceModel().getFileName();
  }

  public static List<String> getAdditionalParametersList(String additionalParameters) {
    return Arrays.stream(additionalParameters.split(","))
            .map(String::trim)
            .filter(s -> !s.isBlank())
            .toList();
  }

}
