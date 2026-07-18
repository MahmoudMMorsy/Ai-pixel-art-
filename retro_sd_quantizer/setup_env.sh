#!/bin/bash
# Script to set up Python virtual environment and install requirements for the SD 1.5 Quantizer.

echo "=== Setting up Retro SD Quantizer Environment ==="

if ! command -v python3 &> /dev/null
then
    echo "❌ Python3 could not be found. Please install Python3 and try again."
    exit 1
fi

# Create virtual environment
echo "Creating virtual environment 'venv'..."
python3 -m venv venv

# Activate virtual environment
source venv/bin/activate

# Upgrade pip
echo "Upgrading pip..."
pip install --upgrade pip

# Install PyTorch (CPU version is sufficient and lightweight)
echo "Installing lightweight PyTorch for CPU..."
pip install torch --index-url https://download.pytorch.org/whl/cpu

# Install requirements
echo "Installing other dependencies from requirements.txt..."
pip install -r requirements.txt

echo "=== Environment Setup Complete! ==="
echo "To activate the environment, run: source venv/bin/activate"
