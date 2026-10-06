#!/bin/bash
echo "========================================"
echo "      CTH TOOLS INVENTORY SYSTEM"
echo "             Version 2.0.0"
echo "========================================"
echo ""
echo "Starting application..."
echo ""
java -jar CTH_Inventory_System.jar
if [ $? -ne 0 ]; then
    echo ""
    echo "========================================"
    echo "  ❌ ERROR: Could not start application"
    echo "========================================"
    echo ""
    echo "Possible issues:"
    echo "1. Java is not installed"
    echo "2. Java is not in PATH"
    echo "3. Missing JAR files"
    echo ""
    echo "To install Java: visit java.com"
    echo ""
    read -p "Press Enter to exit..."
fi