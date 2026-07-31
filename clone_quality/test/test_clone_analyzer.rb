# frozen_string_literal: true

require "minitest/autorun"
require_relative "../lib/clone_analyzer"

class CloneAnalyzerTest < Minitest::Test
  def setup
    @analyzer = CloneQuality::Analyzer.new(
      min_shared_tokens: 1,
      min_shared_statements: 2,
      type3_short_coverage: 0.50,
      type3_span_coverage: 0.50
    )
  end

  def test_type_1_ignores_comments_and_layout
    result = @analyzer.analyze(pair(
      "int add(int a, int b) { return a + b; }",
      "int add(int a,int b) {\n// explanation\nreturn a+b;\n}"
    ))

    assert_equal "CLONE", result["Clone Decision"]
    assert_equal "TYPE_1", result["Clone Type"]
  end

  def test_type_1_finds_complete_fragment_inside_larger_answer
    result = @analyzer.analyze(pair(
      "int add(int a, int b) { return a + b; }",
      "class Example { int add(int a, int b) { return a + b; } void other() {} }"
    ))

    assert_equal "TYPE_1", result["Clone Type"]
    assert_operator result["Coverage SO"].to_f, :<, 1.0
  end

  def test_type_2_requires_consistent_identifier_mapping
    result = @analyzer.analyze(pair(
      "int add(int a, int b) { return a + b; }",
      "long sum(long x, long y) { return x + y; }"
    ))

    assert_equal "CLONE", result["Clone Decision"]
    assert_equal "TYPE_2", result["Clone Type"]
  end

  def test_changed_statement_can_be_type_3
    result = @analyzer.analyze(pair(
      "void copy() { open(); read(); write(); close(); }",
      "void copy() { open(); read(); validate(); write(); close(); }"
    ))

    assert_equal "CLONE", result["Clone Decision"]
    assert_equal "TYPE_3", result["Clone Type"]
    assert_equal "YES", result["Review Required"]
  end

  private

  def pair(github_code, stackoverflow_code)
    {
      "metadata" => {
        "No" => "1",
        "GH Project" => "example",
        "Link SO" => "https://stackoverflow.com/q/1"
      },
      "github" => {
        "path" => "Example.java", "method" => "method",
        "startLine" => 1, "endLine" => github_code.lines.length, "code" => github_code
      },
      "stackoverflow" => {
        "path" => "1.java", "method" => "method",
        "startLine" => 1, "endLine" => stackoverflow_code.lines.length, "code" => stackoverflow_code
      }
    }
  end
end
