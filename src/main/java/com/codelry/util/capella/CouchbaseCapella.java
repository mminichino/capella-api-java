package com.codelry.util.capella;

import java.io.*;
import java.nio.file.*;

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

  private final Path homeDir = Paths.get(System.getProperty("user.home"));
  private final Path configDirectory = homeDir.resolve(CONFIG_DIRECTORY);
  private final Path configFile = configDirectory.resolve(CONFIG_FILE);

  public REST rest;
  public final String profile;
  public String project;

  private String apiHost;
  private String tokenFile;
  private Path tokenFilePath = configDirectory.resolve(DEFAULT_API_KEY_TOKEN);
  private String organization;
  private String accountEmail;
  private String profileKeyId;
  private String profileToken;

  public CouchbaseCapella(String project, String profile) {
    this.profile = (profile != null) ? profile : DEFAULT_PROFILE;
    this.project = (project != null) ? project : DEFAULT_PROFILE;
    LOGGER.debug("using profile: {}", this.profile);
    init();
    this.rest = new REST(this.apiHost, this.profileToken, true);
  }

  private void init() {
    LOGGER.debug("initializing with credential file: {}", configFile.toString());
    readConfig(DEFAULT_PROFILE);
    if (!this.profile.equals(DEFAULT_PROFILE)) {
      readConfig(this.profile);
    }
    readToken();
  }

  private void readConfig(String profile) {
    if (!Files.exists(configFile)) {
      writeDefaultConfig();
    }
    INIConfiguration config = readConfigFile(configFile.toString());
    SubnodeConfiguration profileConfig = config.getSection(profile);
    if (profileConfig == null) {
      throw new RuntimeException("Profile not found: " + profile);
    }
    this.apiHost = getProperty(profileConfig, "api_host", this.apiHost);
    this.tokenFile = getProperty(profileConfig, "token_file", this.tokenFile);
    this.tokenFilePath = configDirectory.resolve(this.tokenFile);
    this.organization = getProperty(profileConfig, "organization", this.organization);
    this.project = getProperty(profileConfig, "project", this.project);
    this.accountEmail = getProperty(profileConfig, "account_email", this.accountEmail);

    LOGGER.debug("found token file: {}", this.tokenFile);
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
    LOGGER.info("using API Key ID: {} from {}", this.profileKeyId, this.tokenFile);
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
              this.profileToken = value;
            } else if (key.equals("APIKeyId")) {
              this.profileKeyId = value;
            }
          }
        }
      } catch (IOException e) {
        throw new RuntimeException("can not read credential file " + this.tokenFile, e);
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

  public String getApiHost() {
    return apiHost;
  }

  public void setApiHost(String apiHost) {
    this.apiHost = apiHost;
  }

  public String getTokenFile() {
    return tokenFile;
  }

  public void setTokenFile(String tokenFile) {
    this.tokenFile = tokenFile;
  }

  public String getOrganization() {
    return organization;
  }

  public void setOrganization(String organization) {
    this.organization = organization;
  }

  public String getProjectName() {
    return project;
  }

  public void setProjectName(String project) {
    this.project = project;
  }

  public String getAccountEmail() {
    return accountEmail;
  }

  public void setAccountEmail(String accountEmail) {
    this.accountEmail = accountEmail;
  }

  public String getProfileKeyId() {
    return profileKeyId;
  }

  public void setProfileKeyId(String profileKeyId) {
    this.profileKeyId = profileKeyId;
  }

  public String getProfileToken() {
    return profileToken;
  }

  public void setProfileToken(String profileToken) {
    this.profileToken = profileToken;
  }
}
