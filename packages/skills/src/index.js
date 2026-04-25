const runtimeSkills = [
  {
    id: "prd-skill",
    name: "PRD Skill",
    category: "analysis",
    packagePath: "packages/skills/prd-skill",
    serverAdapter: "PrdSkillAdapter"
  },
  {
    id: "ui-generate-skill",
    name: "UI Generate Skill",
    category: "design",
    packagePath: "packages/skills/ui-generate-skill",
    serverAdapter: "UiGenerateSkillAdapter"
  },
  {
    id: "code-generate-skill",
    name: "Code Generate Skill",
    category: "implementation",
    packagePath: "packages/skills/code-generate-skill",
    serverAdapter: "FileWriteSkill"
  },
  {
    id: "test-generate-skill",
    name: "Test Generate Skill",
    category: "quality",
    packagePath: "packages/skills/test-generate-skill",
    serverAdapter: "PlaywrightSkill"
  },
  {
    id: "shell-skill",
    name: "Shell Skill",
    category: "execution",
    packagePath: "packages/skills/shell-skill",
    serverAdapter: "ShellSkill"
  },
  {
    id: "file-write-skill",
    name: "File Write Skill",
    category: "execution",
    packagePath: "packages/skills/file-write-skill",
    serverAdapter: "FileWriteSkill"
  },
  {
    id: "report-generate-skill",
    name: "Report Generate Skill",
    category: "delivery",
    packagePath: "packages/skills/report-generate-skill",
    serverAdapter: "TemplateRenderSkill"
  },
  {
    id: "export-skill",
    name: "Export Skill",
    category: "delivery",
    packagePath: "packages/skills/export-skill",
    serverAdapter: "ShellSkill"
  }
];

function findRuntimeSkill(id) {
  return runtimeSkills.find((skill) => skill.id === id);
}

module.exports = {
  runtimeSkills,
  findRuntimeSkill
};
