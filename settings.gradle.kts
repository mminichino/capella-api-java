rootProject.name = "capella-api"

sourceControl {
    gitRepository(java.net.URI.create("https://github.com/mminichino/restfull-core.git")) {
        producesModule("com.codelry.util:restfull-core")
    }
}
