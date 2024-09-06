# Lab 3 -> Networks

- Diego Alonzo 20172
- Arturo Argueta 21527
- Renato Guzmán 21646

## How to run it?

1. Download Docker (if you are in windows enable WSL)

2. open a CLI (command line interface)

3. 
``` bash
git clone https://github.com/Arthurs101/LAB3-NETWORK-S20-UVG-2024.git
```
or 
``` bash
git clone git@github.com:Arthurs101/LAB3-NETWORK-S20-UVG-2024.git
```
4. 
```bash 
cd ./LAB3-NETWORK-S20-UVG-2024
```

5. ls to check the Dockerfile is there

6. 
```bash
docker build -t lab3-networks . && docker run -it lab3-networks 
```
(This will build the image that builds the compiled and then it will run a container attached)

