#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
mkdir -p build/domain-tests
javac -d build/domain-tests app/src/main/java/app/routia/Rules.java app/src/main/java/app/routia/Progress.java tests/RulesTest.java tests/ProgressTest.java app/src/main/java/app/routia/ReminderTimes.java tests/ReminderTimesTest.java app/src/main/java/app/routia/Inputs.java tests/InputsTest.java app/src/main/java/app/routia/TicketRules.java tests/TicketRulesTest.java
java -cp build/domain-tests RulesTest
java -cp build/domain-tests ProgressTest

java -cp build/domain-tests ReminderTimesTest

java -cp build/domain-tests InputsTest

java -cp build/domain-tests TicketRulesTest

javac -cp build/domain-tests -d build/domain-tests app/src/main/java/app/routia/ProgressWidgetData.java tests/ProgressWidgetDataTest.java
java -cp build/domain-tests ProgressWidgetDataTest

javac -cp build/domain-tests -d build/domain-tests app/src/main/java/app/routia/WidgetClickData.java tests/WidgetClickDataTest.java
java -cp build/domain-tests WidgetClickDataTest


javac -cp build/domain-tests -d build/domain-tests app/src/main/java/app/routia/HabitDefaults.java app/src/main/java/app/routia/CelebrationMotion.java tests/HabitDefaultsTest.java tests/CelebrationMotionTest.java
java -cp build/domain-tests HabitDefaultsTest
java -cp build/domain-tests CelebrationMotionTest


javac -cp build/domain-tests -d build/domain-tests app/src/main/java/app/routia/AchievementColors.java tests/AchievementColorsTest.java
java -cp build/domain-tests AchievementColorsTest

javac -cp build/domain-tests -d build/domain-tests tests/RaisedArmsTest.java
java -cp build/domain-tests RaisedArmsTest

javac -cp build/domain-tests -d build/domain-tests app/src/main/java/app/routia/ReportFacts.java app/src/main/java/app/routia/ReportBudget.java app/src/main/java/app/routia/ReportDetail.java tests/ReportDomainTest.java
java -cp build/domain-tests ReportDomainTest


javac -d build/domain-tests app/src/main/java/app/routia/ReportChartData.java tests/ReportChartDataTest.java
java -cp build/domain-tests ReportChartDataTest

javac -d build/domain-tests app/src/main/java/app/routia/ReportProgression.java tests/ReportProgressionTest.java
java -cp build/domain-tests ReportProgressionTest

javac -d build/domain-tests app/src/main/java/app/routia/ReportCatalog.java tests/ReportCatalogTest.java
java -cp build/domain-tests ReportCatalogTest
