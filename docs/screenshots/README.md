# README用スクリーンショット

v1.0.0と画面・機能が同一のRoutia v0.24.1をAndroid 15（API 35）の専用エミュレーターで撮影。
すべて表示用に作った架空の習慣・記録・レビューです。
実端末の画面・記録・認証情報は使っていません。
継続レビューの本文はテストフィクスチャで、実APIの生成結果ではありません。

- today.png：記録前の「今日」
- achievement.png：目標達成後の演出
- progress.png：「成果」の集計
- calendar.png：無料のカレンダー・グラフ画面
- review.png：保存済みレビューの表示例
- reminders.png：回数・時刻の設定
- dark.png：ブラック配色の「今日」

画像は1080×2206のPNGです。実際のネイティブ画面を撮影し、
撮影時にOSのステータスバーとナビゲーションバーを除いています。
アプリ内の表示を書き換えたり、架空のUIを合成したりしていません。

## 撮り直す

空の専用エミュレーターを起動し、`adb devices` で
対象が `emulator-5554` であることを確認してから実行します。
実機や本番パッケージを指定しないでください。

```bash
./gradlew --no-daemon assembleDebug assembleDebugAndroidTest -ProutiaReportQa
adb -s emulator-5554 shell wm size 1080x2400
adb -s emulator-5554 shell wm density 440
adb -s emulator-5554 shell settings put system font_scale 1.0
adb -s emulator-5554 install -r app/build/outputs/apk/debug/app-debug.apk
adb -s emulator-5554 install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
adb -s emulator-5554 shell pm clear app.routia.reportqa
adb -s emulator-5554 shell pm grant app.routia.reportqa android.permission.POST_NOTIFICATIONS
adb -s emulator-5554 shell am instrument -w -e suite readme app.routia.reportqa.test/app.routia.MvpInstrumentation
adb -s emulator-5554 pull /sdcard/Android/data/app.routia.reportqa/files/readme/ docs/screenshots/
```

`ReadmeScreenshots` は、エミュレーター・専用アプリID・空の習慣DB・
APIキー未設定の条件を確認してからサンプルを作ります。
有料生成をオンにせず、外部APIを呼び出しません。
