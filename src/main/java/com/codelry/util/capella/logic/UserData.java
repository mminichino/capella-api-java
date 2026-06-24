package com.codelry.util.capella.logic;

import java.util.List;
import java.util.Objects;

public record UserData(
    String id,
    String name,
    String email,
    String status,
    boolean inactive,
    String organizationId,
    List<String> organizationRoles,
    String lastLogin,
    String region,
    String timeZone,
    Boolean enableNotifications,
    String expiresAt,
    List<ResourcesData> resources,
    AuditData audit
) {
  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    UserData user = (UserData) o;
    return Objects.equals(id, user.id);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id);
  }
}
