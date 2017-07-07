# Installation

Download the artefact (Oracle VirtualBox OVA file) from: 

Size: X GB

The artefact is distributed as a VirtualBox image. To setup the image:
  
  - Download and install VirtualBox: [https://www.virtualbox.org](https://www.virtualbox.org)
  - Open VirtualBox 
  - Click *Machine → Add* and add the OVA file
  - The VM image will appear. Go to *Settings → System → Motherboard* and set at least 8GB of RAM.
  - Start the VM

The VM is running Ubuntu Server 16.04.2 LTS.

The user account is *artefact* and the password is *artefact*.

## Running the Experiment Locally

Optionally, it is possible to run the experiment locally. Please check the *Technical Details* for information about the requirements. 

To run the experiment locally, checkout the github project [https://github.com/MetaBorgCube/deep-conflicts-in-the-wild](https://github.com/MetaBorgCube/deep-conflicts-in-the-wild). The steps to perform the experiment locally are similar to the ones described below. The main difference consists of the UI support to copy/modify/visualize files. 

# Experiment

Below, we describe how the experiment is organized, considering the content of the paper. We also provide instructions for running the experiment and visualizing the data. Finally, we describe how the experiment can be extended. All directories presented in this section are relative to the directory `/home/artefact/deep-conflicts-in-the-wild` in the VM, which is also stored in a global variable `$BASE_DIR` for convenience.

## Organization

The source code of the experiment is organized as follows:

- The folder `Disamb-Experiment` contains the main class used to run the experiment itself (`Disamb-Experiment/src/main/java/main/Main.java`). This class generates the parse tables, creates the parser parsing all files and counts LOC, AST nodes and number of brackets for each file. This folder also contains subfolders for the normalized SDF3 grammars of Java and OCaml `Disamb-Experiment/normalizedGrammars`. The normalized grammars are encoded as [ATerms](http://spoofax.readthedocs.io/en/latest/source/langdev/meta/lang/aterm/terms.html). For reference, the input SDF3 grammars for Java and OCaml are in the subfolder `Disamb-Experiment/sdf3Grammars`. 

- The folder `org.metaborg.sdf2table` contains the SDF3 parse table generator. The parse table generator supports different configurations to generate parse tables that solve the different types of deep priority conflicts, as specified in the paper.

- The folder `org.spoofax.sglr` contains SGLR, an implementation of a scannerless generalized LR parser with support to lazy parse table generation.

- The folder `Calc` contains a Spoofax project for a small Calculator language.

## Running the Experiment

To run the experiment, execute the command `mvn clean verify` on the top-level directory (`$BASE_DIR`). The full experiment takes ~X hours to run. The results are output in the folder `Disamb-Experiment/logs`.

Optionally, it is also possible to run a subset of the experiment, containing just 10 files for each language. To set up the smaller version of the experiment, edit the file `Disamb-Experiment/src/main/java/main/Main.java`, setting the variable `SHORTRUN` to `true`. The smaller version of the experiment takes approximately 5 minutes to run.

We have also included the results of running the experiment in the folder `results`. This folder has the same structure of the resulting  `Disamb-Experiment/logs` directory, and includes the information about:
 
  - `<LangName>-failing-files.txt`: files that failed to parse by SGLR.
  - `<LangName>-files.txt`: files that succeeded to parse.
  - `<LangName>-statistics.txt`: the raw statistics about the language `<LangName>.
  - `verbose.txt`: the console output, i.e., more verbose statistics about the full experiment.

## Visualizing the Data

## Reusability

The experiment is organized such that it can be "easily" extended to consider larger corpuses and other languages. 

### Adding additional files 
 
 To increase the size of the corpuses, copy the additional project to the directory `/Disamb-experiment/test/<LangName>/<ProjectName>`. Note that `<LanguageName>` and the file extension of the additional files should follow the variables defined in `/Disamb-Experiment/src/main/java/main/Main.java`. 

### Including another language

Including another language is somewhat more complicated as the experiment depends on SDF3 normalized grammars, which are generated inside Spoofax projects. We provide instructions on adding a language considering an existing Spoofax project. More instructions on how to define a new language using Spoofax are presented in the [Spoofax Documentation](http://spoofax.readthedocs.io/en/latest/).

We have defined a small language named *Calc*, under the directory `Calc`. Build this project by running `mvn clean verify` on this project, which should produce the normalized files in the subdirectory `Calc/src-gen/syntax/normalized`. The syntax for the language is specified in the file `syntax/Calc.sdf3`.

To include this additional language as part of the experiment, follow the steps:

- Create the directory to copy the normalized grammar to, i.e., create `Disamb-Experiment/normalizedGrammars/<LangName>/normalized` copying the content of `src-gen/syntax/normalized` into it. 

- Edit the file `Disamb-Experiment/src/main/java/main/Main.java`, adding a new entry to the arrays:

    - *languages*: this string must correspond to the subdirectories containing the tests and the normalized grammars. In this example, add the entry `Calc`.
    
    - *extensions*: this string must correspond to the extension of a file of the language. In this case, add the entry `cal`.
    
    - *mainSDF3normModule*: this string must correspond to the name of the main SDF3 file of the language. In this case, add the entry `Calc`.
    
    - *startSymbol*: this string must correspond to the start symbol defined in the SDF3 grammar. In this case, add the entry `Program`.

    - *runExperiment*: a boolean element to indicate whether the language should be tested. Add the entry `true` and optionally set the other values to `false`.
    
    - *createTable*: a boolean element to indicate whether the full parse table should also be generated. Add the entry `true`.
    
    - *testingFile*: this string corresponds to running the experiment on a single file, if the variable `TESTING` is set to true. Add the entry `example.cal` if `TESTING` is set to true, otherwise, add a different filename with the `cal` extension.

- The files to be tested should be added to the test folder. In this case, create a directory `Disamb-Experiment/test/Calc/example` and copy the file `Calc/example/example.cal` from the calc project into it. 

- It is also necessary to add logging support for the new language. The file `Disamb-Experiment/src/main/resources/log4j.properties` contains the information about all the loggers and also a template for creating a logger for a new language. The properties for the *Calc* language are already set up.

To run the experiment only for this new language and for the testing file, set the array `runExperiment` accordingly and the variable `TESTING` to true. Finally, run `mvn clean verify` on the top-level folder (`BASE_DIR`) to produce the statistics for the new language.

# Technical Details

The following packages have already been installed on the virtual machine:

      sudo apt-get install openjdk-8-jdk
      sudo apt install maven

## SSH connection

All configuration for making an SSH connection should already be in place when the VM image is imported from the OVA format. 

There are several options for network settings in Virtual Box. One of the options that allows for SSH connection together with the internet connection available in the VM is using NAT + Port forwarding.

In the VirtualBox click Settings → Network → Choose Adapter 1 → switch to NAT → Expand Advanced → Port Forwarding and fill in the table such that host port *3022* will be forwarded to guest port *22*, naming the entry *ssh*. 

To SSH into the guest VM, run:

      ssh -p 3022 artefact@127.0.0.1


