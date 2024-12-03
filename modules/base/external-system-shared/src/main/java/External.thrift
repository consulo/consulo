namespace * consulo.externalSystem.shared

struct ExternalProject {
    1:string name;

    2:optional string qname;

    3:optional string description;

    4:optional string group;

    5:optional string version;

    6:optional map<string, ExternalProject> childProjects;

    7:map<string, ExternalSourceSet> sourceSts;

    8:string projectDir;

    9:string buildDir;

    10:string buildFile;

    11:map<string, string> properties;
    
}

struct ExternalSourceSet {
    1:string name;

    2:map<ExternalSourceType, ExternalSourceDirectorySet> sources;
}

struct ExternalSourceDirectorySet {
    1:string name;

    2:set<string> srcDirs;

    3:optional string outputDir;

    4:set<string> excludes;

    5:set<string> includes;
}

struct ExternalSourceType {
    1: string name;

    2: set<string> flags;
}

const string SOURCE_FLAG_PRODUCTION = "prod";
const string SOURCE_FLAG_TEST = "test";
const string SOURCE_FLAG_RESOURCE = "res";
const string SOURCE_FLAG_EXCLUDED = "excluded";

const ExternalSourceType PRODUCTION = {"name": "PRODUCTION", "flags": ["prod"]};

const ExternalSourceType PRODUCTION_RESOURCE = {"name": "PRODUCTION_RESOURCE", "flags": ["prod", "res"]};

const ExternalSourceType TEST = {"name": "TEST", "flags": ["test"]};

const ExternalSourceType TEST_RESOURCE = {"name": "TEST_RESOURCE", "flags": ["test", "res"]};

const ExternalSourceType EXCLUDED = {"name": "EXCLUDED", "flags": ["excluded"]};

service ExternalService {

}