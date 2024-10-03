package com.codelry.util.capella;

import java.io.*;
import java.nio.file.*;
import java.util.Properties;

import org.apache.commons.configuration2.INIConfiguration;
import org.apache.commons.configuration2.SubnodeConfiguration;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.codelry.util.rest.REST;

public class CouchbaseCapella {
  private static final Logger LOGGER = LogManager.getLogger(CouchbaseCapella.class);
  private static final String CONFIG_DIRECTORY = ".capella";
  private static final String CONFIG_FILE = "credentials";
  private static final String DEFAULT_API_KEY_TOKEN = "default-api-key-token.txt";
  private static final String DEFAULT_PROFILE = "default";
  private static CouchbaseCapella instance;

  public static final String CAPELLA_ORGANIZATION_NAME = "capella.organization.name";
  public static final String CAPELLA_ORGANIZATION_ID = "capella.organization.id";
  public static final String CAPELLA_PROJECT_NAME = "capella.project.name";
  public static final String CAPELLA_PROJECT_ID = "capella.project.id";
  public static final String CAPELLA_DATABASE_NAME = "capella.database.name";
  public static final String CAPELLA_DATABASE_ID = "capella.database.id";
  public static final String CAPELLA_COLUMNAR_NAME = "capella.columnar.name";
  public static final String CAPELLA_COLUMNAR_ID = "capella.columnar.id";
  public static final String CAPELLA_TOKEN = "capella.token";
  public static final String CAPELLA_API_HOST = "capella.api.host";
  public static final String CAPELLA_USER_EMAIL = "capella.user.email";
  public static final String CAPELLA_USER_ID = "capella.user.id";

  public static final String CAPELLA_DEFAULT_PROJECT_NAME = "default";
  public static final String CAPELLA_DEFAULT_API_HOST = "cloudapi.cloud.couchbase.com";

  private static final Path homeDir = Paths.get(System.getProperty("user.home"));
  private static final Path configDirectory = homeDir.resolve(CONFIG_DIRECTORY);
  private static final Path configFile = configDirectory.resolve(CONFIG_FILE);

  public static REST rest;
  public static String profile;
  public static String organizationName;
  public static String organizationId;
  public static String projectName;
  public static String projectId;
  public static String databaseName;
  public static String databaseId;
  public static String columnarName;
  public static String columnarId;

  private static String apiHost;
  private static String tokenFile;
  private static Path tokenFilePath = configDirectory.resolve(DEFAULT_API_KEY_TOKEN);
  private static String organization;
  private static String accountEmail;
  private static String accountId;
  private static String profileKeyId;
  private static String profileToken;

  private CouchbaseCapella() {}

  public static CouchbaseCapella getInstance(String project, String profile) {
    if (instance == null) {
      instance = new CouchbaseCapella();
      instance.init(project, profile);
    }
    return instance;
  }

  public static CouchbaseCapella getInstance(Properties properties) {
    if (instance == null) {
      instance = new CouchbaseCapella();
      instance.init(properties);
    }
    return instance;
  }

  public void init(String project, String profile) {
    CouchbaseCapella.profile = (profile != null) ? profile : DEFAULT_PROFILE;
    CouchbaseCapella.projectName = (project != null) ? project : DEFAULT_PROFILE;
    LOGGER.debug("using profile: {}", CouchbaseCapella.profile);
    processConfig();
    rest = new REST(apiHost, profileToken, true);
  }

  public void init(Properties properties) {
    processProperties(properties);
    if (profileToken == null) {
      throw new RuntimeException(String.format("please set property %s to provide the API v4 token", CAPELLA_TOKEN));
    }
    rest = new REST(apiHost, profileToken, true);
  }

  private void processConfig() {
    LOGGER.debug("initializing with credential file: {}", configFile.toString());
    readConfig(DEFAULT_PROFILE);
    if (!profile.equals(DEFAULT_PROFILE)) {
      readConfig(profile);
    }
    readToken();
  }

  private void processProperties(Properties properties) {
    CouchbaseCapella.organizationName = properties.getProperty(CAPELLA_ORGANIZATION_NAME);
    CouchbaseCapella.organizationId = properties.getProperty(CAPELLA_ORGANIZATION_ID);
    CouchbaseCapella.projectName = properties.getProperty(CAPELLA_PROJECT_NAME, CAPELLA_DEFAULT_PROJECT_NAME);
    CouchbaseCapella.projectId = properties.getProperty(CAPELLA_PROJECT_ID);
    CouchbaseCapella.databaseName = properties.getProperty(CAPELLA_DATABASE_NAME);
    CouchbaseCapella.databaseId = properties.getProperty(CAPELLA_DATABASE_ID);
    CouchbaseCapella.columnarName = properties.getProperty(CAPELLA_COLUMNAR_NAME);
    CouchbaseCapella.columnarId = properties.getProperty(CAPELLA_COLUMNAR_ID);
    CouchbaseCapella.accountEmail = properties.getProperty(CAPELLA_USER_EMAIL);
    CouchbaseCapella.accountId = properties.getProperty(CAPELLA_USER_ID);
    apiHost = properties.getProperty(CAPELLA_API_HOST, CAPELLA_DEFAULT_API_HOST);
    profileToken = properties.getProperty(CAPELLA_TOKEN);
  }

  private void readConfig(String profile) {
    LOGGER.debug(" -> reading profile: {}", profile);
    if (!Files.exists(configFile)) {
      writeDefaultConfig();
    }
    INIConfiguration config = readConfigFile(configFile.toString());
    SubnodeConfiguration profileConfig = config.getSection(profile);
    if (profileConfig == null) {
      throw new RuntimeException("Profile not found: " + profile);
    }
    apiHost = getProperty(profileConfig, "api_host", apiHost);
    tokenFile = getProperty(profileConfig, "token_file", tokenFile);
    tokenFilePath = configDirectory.resolve(tokenFile);
    organization = getProperty(profileConfig, "organization", organization);
    projectName = getProperty(profileConfig, "project", projectName);
    accountEmail = getProperty(profileConfig, "account_email", accountEmail);

    LOGGER.debug("Token File Path: {}", tokenFilePath);
    LOGGER.debug("Organization: {}", organization);
    LOGGER.debug("Project: {}", projectName);
    LOGGER.debug("Email: {}", accountEmail);
  }

  private String getProperty(SubnodeConfiguration config, String key, String defaultValue) {
    return config.getProperty(key) != null ? config.getProperty(key).toString() : defaultValue;
  }

  private INIConfiguration readConfigFile(String configFile) {
    INIConfiguration iniConfiguration = new INIConfiguration();
    try (FileReader fileReader = new FileReader(configFile)) {
      iniConfiguration.read(fileReader);
      return iniConfiguration;
    } catch (IOException | ConfigurationException e) {
      throw new RuntimeException("can not read config file " + configFile, e);
    }
  }

  private void writeDefaultConfig() {
    INIConfiguration iniConfiguration = new INIConfiguration();
    SubnodeConfiguration defaultConfig = iniConfiguration.getSection("default");
    defaultConfig.addProperty("api_host", "cloudapi.cloud.couchbase.com");
    defaultConfig.addProperty("token_file", DEFAULT_API_KEY_TOKEN);

    try {
      if (!Files.exists(configDirectory)) {
        Files.createDirectory(configDirectory);
      }
      try (FileWriter fileWriter = new FileWriter(configFile.toString())) {
        iniConfiguration.write(fileWriter);
      }
    } catch (IOException | ConfigurationException e) {
      throw new RuntimeException("can not write config file " + configFile, e);
    }
  }

  private void readToken() {
    if (!Files.exists(tokenFilePath)) {
      findTokenFile();
    }
    readTokenFile();
    LOGGER.debug("using API Key ID: {} from {}", profileKeyId, tokenFile);
  }

  private void readTokenFile() {
    if (Files.exists(tokenFilePath)) {
      try (BufferedReader reader = Files.newBufferedReader(tokenFilePath)) {
        String line;
        while ((line = reader.readLine()) != null) {
          String[] parts = line.split(":", 2);
          if (parts.length == 2) {
            String key = parts[0].trim();
            String value = parts[1].trim();
            if (key.equals("APIKeyToken")) {
              profileToken = value;
            } else if (key.equals("APIKeyId")) {
              profileKeyId = value;
            }
          }
        }
      } catch (IOException e) {
        throw new RuntimeException("can not read credential file " + tokenFile, e);
      }
    } else {
      throw new RuntimeException("Please create Capella token file (i.e. $HOME/.capella/default-api-key-token.txt)");
    }
  }

  private void findTokenFile() {
    Path downloadDir = homeDir.resolve("Downloads");

    try {
      if (Files.exists(downloadDir.resolve(tokenFile))) {
        Files.copy(downloadDir.resolve(tokenFile), tokenFilePath, StandardCopyOption.REPLACE_EXISTING);
      } else if (Files.exists(homeDir.resolve(tokenFile))) {
        Files.copy(homeDir.resolve(tokenFile), tokenFilePath, StandardCopyOption.REPLACE_EXISTING);
      }
    } catch (IOException e) {
      throw new RuntimeException("can not copy token file", e);
    }
  }

  public static String getApiHost() {
    return apiHost;
  }

  public static void setApiHost(String apiHost) {
    CouchbaseCapella.apiHost = apiHost;
  }

  public static String getTokenFile() {
    return tokenFile;
  }

  public static void setTokenFile(String tokenFile) {
    CouchbaseCapella.tokenFile = tokenFile;
  }

  public static String getOrganization() {
    return organization;
  }

  public static void setOrganization(String organization) {
    CouchbaseCapella.organization = organization;
  }

  public static String getProjectName() {
    return projectName;
  }

  public static void setProjectName(String project) {
    CouchbaseCapella.projectName = project;
  }

  public static String getAccountId() {
    return accountId;
  }

  public static void setAccountId(String accountId) {
    CouchbaseCapella.accountId = accountId;
  }

  public static String getAccountEmail() {
    return accountEmail;
  }

  public static void setAccountEmail(String accountEmail) {
    CouchbaseCapella.accountEmail = accountEmail;
  }

  public static String getProfileKeyId() {
    return profileKeyId;
  }

  public static void setProfileKeyId(String profileKeyId) {
    CouchbaseCapella.profileKeyId = profileKeyId;
  }

  public static String getProfileToken() {
    return profileToken;
  }

  public static void setProfileToken(String profileToken) {
    CouchbaseCapella.profileToken = profileToken;
  }

  public static boolean hasAccountId() {
    return accountId != null;
  }

  public static boolean hasAccountEmail() {
    return accountEmail != null;
  }

  public static boolean hasOrganizationName() {
    return organizationName != null;
  }

  public static boolean hasProjectName() {
    return projectName != null;
  }

  public static boolean hasDatabaseName() {
    return databaseName != null;
  }

  public static boolean hasColumnarName() {
    return columnarName != null;
  }

  public static boolean hasOrganizationId() {
    return organizationId != null;
  }

  public static boolean hasProjectId() {
    return projectId != null;
  }

  public static boolean hasDatabaseId() {
    return databaseId != null;
  }

  public static boolean hasColumnarId() {
    return columnarId != null;
  }

  public static String getOrganizationName() {
    return organizationName;
  }

  public static void setOrganizationName(String organizationName) {
    CouchbaseCapella.organizationName = organizationName;
  }

  public static String getOrganizationId() {
    return organizationId;
  }

  public static void setOrganizationId(String organizationId) {
    CouchbaseCapella.organizationId = organizationId;
  }

  public static String getProjectId() {
    return projectId;
  }

  public static void setProjectId(String projectId) {
    CouchbaseCapella.projectId = projectId;
  }

  public static String getDatabaseName() {
    return databaseName;
  }

  public static void setDatabaseName(String databaseName) {
    CouchbaseCapella.databaseName = databaseName;
  }

  public static String getDatabaseId() {
    return databaseId;
  }

  public static void setDatabaseId(String databaseId) {
    CouchbaseCapella.databaseId = databaseId;
  }

  public static String getColumnarName() {
    return columnarName;
  }

  public static void setColumnarName(String columnarName) {
    CouchbaseCapella.columnarName = columnarName;
  }

  public static String getColumnarId() {
    return columnarId;
  }

  public static void setColumnarId(String columnarId) {
    CouchbaseCapella.columnarId = columnarId;
  }
}
