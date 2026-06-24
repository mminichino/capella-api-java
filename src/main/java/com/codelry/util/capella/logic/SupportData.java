package com.codelry.util.capella.logic;

import com.fasterxml.jackson.annotation.JsonProperty;

public record SupportData(
    String plan,
    @JsonProperty("timezone") String timezone
) {}
