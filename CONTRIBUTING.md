# Setting up
## 1. Clone this repository into your workspace
```sh
git clone https://github.com/MinecraftTAS/TASmod.git
```
## 2. Import root project into your IDE
### Eclipse
1. Set your workspace folder to the parent folder of the TASmod folder, so if you have something like `workspace/TASmod`, set the workspace folder to "TASmod".
2. Click `File>Import` then search for "Existing Gradle project" and click next
3. Select the "TASmod" folder as a project root directory then click finish
### IntelliJ
Use `File>Open`, then select the `TASmod` folder
## 3. Decompile Source
Run the gradle task `genSources` either in your IDE or by running
```sh
./gradlew genSources
```
# Run Minecraft in Dev-Environment
## Eclipse
1. Run the gradle task `eclipse`
2. Select the project, right click, then gradle>Refresh Gradle Project
3. Select the TASmod_client.launch file then click the run button or press Ctrl+F11
## IntelliJ
Run the gradle task `runClient`

# Building
1. Run the gradle task `build`
```sh
./gradlew build
```
2. The generated jar is in `build/libs`