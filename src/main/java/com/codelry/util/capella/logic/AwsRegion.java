package com.codelry.util.capella.logic;

public enum AwsRegion {
  US_EAST_1("us-east-1", "US East (N. Virginia)"),
  US_EAST_2("us-east-2", "US East (Ohio)"),
  US_WEST_1("us-west-1", "US West (N. California)"),
  US_WEST_2("us-west-2", "US West (Oregon)"),
  CA_CENTRAL_1("ca-central-1", "Canada (Central)"),
  EU_WEST_1("eu-west-1", "EU (Ireland)"),
  EU_WEST_2("eu-west-2", "EU (London)"),
  EU_WEST_3("eu-west-3", "EU (Paris)"),
  EU_CENTRAL_1("eu-central-1", "EU (Frankfurt)"),
  EU_NORTH_1("eu-north-1", "EU (Stockholm)"),
  AP_SOUTH_1("ap-south-1", "Asia Pacific (Mumbai)"),
  AP_SOUTHEAST_1("ap-southeast-1", "Asia Pacific (Singapore)"),
  AP_SOUTHEAST_2("ap-southeast-2", "Asia Pacific (Sydney)"),
  AP_NORTHEAST_1("ap-northeast-1", "Asia Pacific (Tokyo)"),
  AP_NORTHEAST_2("ap-northeast-2", "Asia Pacific (Seoul)"),
  AP_EAST_1("ap-east-1", "Asia Pacific (Hong Kong)"),
  SA_EAST_1("sa-east-1", "South America (São Paulo)"),
  ME_SOUTH_1("me-south-1", "Middle East (Bahrain)");

  private final String regionCode;
  private final String description;

  AwsRegion(String regionCode, String description) {
    this.regionCode = regionCode;
    this.description = description;
  }

  public String getRegionCode() {
    return regionCode;
  }

  public String getDescription() {
    return description;
  }

  public AwsRegion fromString(String region) {
    for (AwsRegion awsRegion : AwsRegion.values()) {
      if (awsRegion.getRegionCode().equals(region)) {
        return awsRegion;
      }
    }
    throw new IllegalArgumentException("Unknown region: " + region);
  }

  @Override
  public String toString() {
    return regionCode;
  }
}
