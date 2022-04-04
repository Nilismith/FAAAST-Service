/*
 * Copyright (c) 2021 Fraunhofer IOSB, eine rechtlich nicht selbstaendige
 * Einrichtung der Fraunhofer-Gesellschaft zur Foerderung der angewandten
 * Forschung e.V.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.fraunhofer.iosb.ilt.faaast.service.starter;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import de.fraunhofer.iosb.ilt.faaast.service.Service;
import de.fraunhofer.iosb.ilt.faaast.service.config.ServiceConfig;
import de.fraunhofer.iosb.ilt.faaast.service.exception.InvalidConfigurationException;
import de.fraunhofer.iosb.ilt.faaast.service.starter.util.AASEnvironmentHelper;
import de.fraunhofer.iosb.ilt.faaast.service.starter.util.ServiceConfigHelper;
import de.fraunhofer.iosb.ilt.faaast.service.util.LambdaExceptionHelper;
import io.adminshell.aas.v3.dataformat.DeserializationException;
import io.adminshell.aas.v3.model.AssetAdministrationShellEnvironment;
import io.adminshell.aas.v3.model.validator.ShaclValidator;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.jena.shacl.ValidationReport;
import org.apache.jena.shacl.lib.ShLib;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.Spec;


/**
 * Class for configuring and starting a FA³ST Service
 */
@Command(name = "FA³ST Service Starter", mixinStandardHelpOptions = true, version = "0.1", description = "Starts a FA³ST Service")
public class App implements Runnable {

    private static final Logger LOGGER = LoggerFactory.getLogger(App.class);
    private static final int INDENT_DEFAULT = 20;
    private static final int INDENT_STEP = 3;
    private static final CountDownLatch SHUTDOWN_REQUESTED = new CountDownLatch(1);
    private static final CountDownLatch SHUTDOWN_FINISHED = new CountDownLatch(1);
    private static final ObjectMapper mapper = new ObjectMapper()
            .enable(SerializationFeature.INDENT_OUTPUT)
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .setSerializationInclusion(JsonInclude.Include.NON_EMPTY);
    private static AtomicReference<Service> serviceRef = new AtomicReference<>();
    // commands
    protected static final String COMMAND_CONFIG = "--config";
    protected static final String COMMAND_MODEL = "--model";
    // model
    protected static final String MODEL_FILENAME_DEFAULT = "aasenvironment.*";
    protected static final String MODEL_FILENAME_PATTERN = "aasenvironment\\..*";
    // config
    protected static final String CONFIG_FILENAME_DEFAULT = "config.json";
    // environment
    protected static final String ENV_PATH_SEPERATOR = ".";
    protected static final String ENV_FAAAST_KEY = "faaast";
    protected static final String ENV_MODEL_KEY = "model";
    protected static final String ENV_CONFIG_KEY = "config";
    protected static final String ENV_EXTENSION_KEY = "extension";

    protected static final String ENV_CONFIG_EXTENSION_PREFIX = envPath(ENV_FAAAST_KEY, ENV_CONFIG_KEY, ENV_EXTENSION_KEY, "");
    protected static final String ENV_MODEL_FILE_PATH = envPath(ENV_FAAAST_KEY, ENV_MODEL_KEY);
    protected static final String ENV_CONFIG_FILE_PATH = envPath(ENV_FAAAST_KEY, ENV_CONFIG_KEY);

    @Spec
    private CommandSpec spec;

    private Service service;

    private static String indent(String value, int steps) {
        return String.format(String.format("%%%ds%s", INDENT_DEFAULT + (INDENT_STEP * steps), value), "");
    }


    private static String envPath(String... args) {
        return Stream.of(args).collect(Collectors.joining(ENV_PATH_SEPERATOR));
    }


    public static void main(String[] args) {
        LOGGER.info("Starting FA³ST Service...");
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                SHUTDOWN_REQUESTED.countDown();
                try {
                    SHUTDOWN_FINISHED.await();
                }
                catch (InterruptedException ex) {
                    LOGGER.error("Error while waiting for FA³ST Service to gracefully shutdown");
                    Thread.currentThread().interrupt();
                }
                finally {
                    LOGGER.info("Goodbye!");
                }
            }
        });
        new CommandLine(new App())
                .setCaseInsensitiveEnumValuesAllowed(true)
                .execute(args);
        try {
            SHUTDOWN_REQUESTED.await();
        }
        catch (InterruptedException e) {
            LOGGER.error("Interrupted!", e);
            Thread.currentThread().interrupt();
        }
        finally {
            LOGGER.info("Shutting down FA³ST Service...");
            if (serviceRef.get() != null) {
                serviceRef.get().stop();
            }
            LOGGER.info("FA³ST Service successfully shut down");
            SHUTDOWN_FINISHED.countDown();
        }
    }

    @Option(names = {
            "-m",
            COMMAND_MODEL
    }, description = "Asset Administration Shell Environment FilePath. Default Value = ${DEFAULT-VALUE}", defaultValue = MODEL_FILENAME_DEFAULT)
    public File modelFile;

    @Option(names = {
            "-c",
            COMMAND_CONFIG
    }, description = "The config file path. Default Value = ${DEFAULT-VALUE}", defaultValue = CONFIG_FILENAME_DEFAULT)
    public File configFile;

    @Option(names = "--no-autoCompleteConfig", negatable = true, description = "Autocompletes the configuration with default values for required configuration sections. True by default")
    public boolean autoCompleteConfiguration = true;

    @Option(names = "--endpoint", split = ",", description = "Additional endpoints that should be started.")
    public List<EndpointType> endpoints = new ArrayList<>();

    @Parameters(description = "Additional properties to override values of configuration using JSONPath notation withtout starting '$.' (see https://goessner.net/articles/JsonPath/)")
    public Map<String, String> properties = new HashMap<>();

    @Option(names = "--emptyModel", description = "Starts the FA³ST service with an empty Asset Administration Shell Environment. False by default")
    public boolean useEmptyModel = false;

    @Option(names = "--no-modelValidation", negatable = true, description = "Validates the AAS Environment. True by default")
    public boolean validateModel = true;

    @Override
    public void run() {
        printHeader();
        ServiceConfig config = null;
        try {
            config = getConfig();
        }
        catch (IOException e) {
            LOGGER.error("Error loading config file", e);
            return;
        }
        AssetAdministrationShellEnvironment model = null;
        try {
            model = getModel();
        }
        catch (DeserializationException e) {
            LOGGER.error("Error loading model file", e);
            return;
        }
        if (validateModel) {
            try {
                if (!validate(model)) {
                    return;
                }
            }
            catch (IOException e) {
                LOGGER.error("Unexpected exception with validating model", e);
            }
        }
        if (autoCompleteConfiguration) {
            ServiceConfigHelper.autoComplete(config);
        }
        try {
            ServiceConfigHelper.apply(config, endpoints.stream()
                    .map(LambdaExceptionHelper.rethrowFunction(
                            x -> x.getImplementation().getDeclaredConstructor().newInstance()))
                    .collect(Collectors.toList()));
        }
        catch (InvalidConfigurationException | ReflectiveOperationException e) {
            LOGGER.error("Adding endpoints to config failed", e);
            return;
        }
        try {
            config = ServiceConfigHelper.withProperties(config, getConfigOverrides());
        }
        catch (JsonProcessingException e) {
            LOGGER.error("Overriding config properties failed", e);
            return;
        }
        try {
            service = new Service(model, config);
            LOGGER.info("Starting FA³ST Service...");
            LOGGER.debug("Using configuration file: ");
            printConfig(config);
            service.start();
            LOGGER.info("FA³ST Service successfully started");
            LOGGER.info("Press CTRL + C to stop");
        }
        catch (Exception e) {
            LOGGER.error("Unexpected exception encountered while execution FA³ST Service", e);
        }
    }


    private void printConfig(ServiceConfig config) {
        if (LOGGER.isDebugEnabled()) {
            try {
                LOGGER.debug(mapper.writeValueAsString(config));
            }
            catch (JsonProcessingException e) {
                LOGGER.debug("Printing config failed", e);
            }
        }
    }


    private void printHeader() {
        LOGGER.info("            _____                                                       ");
        LOGGER.info("           |___ /                                                       ");
        LOGGER.info(" ______      |_ \\    _____ _______     _____                 _          ");
        LOGGER.info("|  ____/\\   ___) | / ____|__   __|    / ____|               (_)         ");
        LOGGER.info("| |__ /  \\ |____/ | (___    | |      | (___   ___ _ ____   ___  ___ ___ ");
        LOGGER.info("|  __/ /\\ \\        \\___ \\   | |       \\___ \\ / _ \\ '__\\ \\ / / |/ __/ _ \\");
        LOGGER.info("| | / ____ \\       ____) |  | |       ____) |  __/ |   \\ V /| | (_|  __/");
        LOGGER.info("|_|/_/    \\_\\     |_____/   |_|      |_____/ \\___|_|    \\_/ |_|\\___\\___|");
        LOGGER.info("");
        LOGGER.info("-------------------------------------------------------------------------");
    }


    private ServiceConfig getConfig() throws IOException {
        if (spec.commandLine().getParseResult().hasMatchedOption(COMMAND_CONFIG)) {
            LOGGER.info("Config: {} (CLI)", configFile.getAbsoluteFile());
            return ServiceConfigHelper.load(configFile);
        }
        if (System.getenv(ENV_CONFIG_FILE_PATH) != null && !System.getenv(ENV_CONFIG_FILE_PATH).isBlank()) {
            LOGGER.info("Config: {} (ENV)", System.getenv(ENV_CONFIG_FILE_PATH));
            configFile = new File(System.getenv(ENV_CONFIG_FILE_PATH));
            return ServiceConfigHelper.load(new File(System.getenv(ENV_CONFIG_FILE_PATH)));
        }
        if (new File(CONFIG_FILENAME_DEFAULT).exists()) {
            configFile = new File(CONFIG_FILENAME_DEFAULT);
            LOGGER.info("Config: {} (default location)", configFile.getAbsoluteFile());
            return ServiceConfigHelper.load(configFile);
        }
        LOGGER.info("Config: empty (default)");
        return ServiceConfigHelper.DEFAULT_SERVICE_CONFIG;
    }


    private AssetAdministrationShellEnvironment getModel() throws DeserializationException {
        if (useEmptyModel) {
            LOGGER.info("Model: empty (CLI)");
            if (validateModel) {
                LOGGER.info("Model validation is disabled when using empty model");
                validateModel = false;
            }
            return AASEnvironmentHelper.EMPTY_AAS;
        }
        if (spec.commandLine().getParseResult().hasMatchedOption(COMMAND_MODEL)) {
            LOGGER.info("Model: {} (CLI)", modelFile.getAbsoluteFile());
            return AASEnvironmentHelper.fromFile(modelFile);
        }
        if (System.getenv(ENV_MODEL_FILE_PATH) != null && !System.getenv(ENV_MODEL_FILE_PATH).isBlank()) {
            LOGGER.info("Model: {} (ENV: {})", modelFile.getAbsoluteFile(), System.getenv(ENV_MODEL_FILE_PATH));
            modelFile = new File(System.getenv(ENV_MODEL_FILE_PATH));
            return AASEnvironmentHelper.fromFile(new File(System.getenv(ENV_MODEL_FILE_PATH)));
        }
        Optional<File> defaultModel = findDefaultModel();
        if (defaultModel.isPresent()) {
            LOGGER.info("Model: {} (default location)", defaultModel.get().getAbsoluteFile());
            return AASEnvironmentHelper.fromFile(defaultModel.get());
        }
        LOGGER.info("Model: empty (default)");
        return AASEnvironmentHelper.EMPTY_AAS;
    }


    private Optional<File> findDefaultModel() {
        try {
            List<File> modelFiles;
            try (Stream<File> stream = Files.find(Paths.get(""), 1,
                    (file, attributes) -> file.toFile()
                            .getName()
                            .matches(MODEL_FILENAME_PATTERN))
                    .map(Path::toFile)) {
                modelFiles = stream.collect(Collectors.toList());
            }
            if (modelFiles.size() > 1 && LOGGER.isWarnEnabled()) {
                LOGGER.warn("Found multiple model files matching the default pattern. To use a specific one use command '{} <filename>' (files found: {}, file pattern: {})",
                        COMMAND_MODEL,
                        modelFiles.stream()
                                .map(File::getName)
                                .collect(Collectors.joining(",", "[", "]")),
                        MODEL_FILENAME_PATTERN);
            }
            return modelFiles.stream().findFirst();
        }
        catch (IOException ex) {
            return Optional.empty();
        }
    }


    protected Map<String, String> getConfigOverrides() {
        Map<String, String> envParameters = System.getenv().entrySet().stream()
                .filter(x -> x.getKey().startsWith(ENV_CONFIG_EXTENSION_PREFIX))
                .filter(x -> !properties.containsKey(x.getKey().substring(ENV_CONFIG_EXTENSION_PREFIX.length() - 1)))
                .collect(Collectors.toMap(
                        x -> x.getKey().substring(ENV_CONFIG_EXTENSION_PREFIX.length()),
                        Entry::getValue));
        Map<String, String> result = new HashMap<>(envParameters);
        for (var property: properties.entrySet()) {
            if (property.getKey().startsWith(ENV_CONFIG_EXTENSION_PREFIX)) {
                String realKey = property.getKey().substring(ENV_CONFIG_EXTENSION_PREFIX.length());
                LOGGER.info("Found unneccessary prefix for CLI parameter '{}' (remove prefix '{}' to not receive this message any longer)", realKey, ENV_CONFIG_EXTENSION_PREFIX);
                result.put(realKey, property.getValue());
            }
            else {
                result.put(property.getKey(), property.getValue());
            }
        }
        if (!result.isEmpty()) {
            LOGGER.info("Overriding config parameter: {}{}",
                    System.lineSeparator(),
                    result.entrySet().stream()
                            .map(x -> indent(
                                    String.format("%s=%s [%s]",
                                            x.getKey(),
                                            x.getValue(),
                                            properties.containsKey(x.getKey()) ? "CLI" : "ENV"),
                                    1))
                            .collect(Collectors.joining(System.lineSeparator())));
        }
        return result;
    }


    private boolean validate(AssetAdministrationShellEnvironment aasEnv) throws IOException {
        LOGGER.debug("Validating model...");
        ShaclValidator shaclValidator = ShaclValidator.getInstance();
        ValidationReport report = shaclValidator.validateGetReport(aasEnv);
        if (report.conforms()) {
            LOGGER.info("Model successfully validated");
            return true;
        }
        ByteArrayOutputStream validationResultStream = new ByteArrayOutputStream();
        ShLib.printReport(validationResultStream, report);
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("Model validation failed with the following error(s):{}{}", System.lineSeparator(), validationResultStream);
        }
        return false;
    }
}
