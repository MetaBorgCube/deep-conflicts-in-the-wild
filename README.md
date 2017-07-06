# Installation

Download the artifact (Oracle VirtualBox OVA file) from: 

Size: 

The artefact is distributed as a VirtualBox image. To setup the image:
  
  - Download and install VirtualBox [https://www.virtualbox.org](https://www.virtualbox.org)
  - Open VirtualBox 
  - Click *Machine → Add* and add the OVA file
  - The VM image will appear. Go to *Settings → System → Motherboard* and set at least 8GB of RAM.
  - Start the VM

The VM is running Ubuntu Server 16.04.2 LTS.

The user account is *artefact* and the password is *artefact*.

# Experiment

Below, we describe how the experiment is organized, considering the content of the paper. We also provide instructions for running the experiment and visualizing the data. Finally, we describe how the experiment can be extended to consider larger corpuses.

## Organization

## Running the Experiment

## Visualizing the Data

## Reusability

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

