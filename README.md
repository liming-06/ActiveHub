# ActiveHub Sports Centre

Java console project for DIT2123 Object-Oriented Modelling.

ActiveHub manages sports facility bookings, equipment rentals, bills, promotions, payments and daily reports. Records are saved in a local text file.

## Requirements

JDK 17 or later. No external libraries are needed.

## Run in IntelliJ IDEA

1. Download or clone this repository and open the ActiveHub project folder.
2. Set the project SDK to your installed JDK 17 or later.
3. Run `activehub.Main` using the **ActiveHub** configuration.
4. Run `activehub.SelfTest` using **ActiveHub_checks** to check the program.

Use the project folder as the working directory.

## Run from a terminal

```sh
mkdir -p out/production/ActiveHub
javac --release 17 -d out/production/ActiveHub src/activehub/*.java
java -cp out/production/ActiveHub activehub.Main
```

Run the regression checks:

```sh
java -cp out/production/ActiveHub activehub.SelfTest
```

## Project files

- `src/activehub/` — 23 Java source files.
- `data/activehub.txt` — initial empty data file.
- `data/demo.txt` — sample demonstration records.
- `ActiveHub.iml` and `.idea/` — IntelliJ project and run configurations.

Card and e-wallet payments are recorded locally and do not connect to payment providers.
