import pandas as pd
import pdfplumber
import os

docs_path = r"D:\UTP\SISTEMAS\PROYECTO UTP1\archivos"

def read_pdf(filename):
    print(f"\n--- {filename} ---")
    try:
        with pdfplumber.open(os.path.join(docs_path, filename)) as pdf:
            for page in pdf.pages:
                print(page.extract_text())
    except Exception as e:
        print(f"Error reading {filename}: {e}")

def read_excel(filename):
    print(f"\n--- {filename} ---")
    try:
        xl = pd.ExcelFile(os.path.join(docs_path, filename))
        for sheet_name in xl.sheet_names:
            print(f"\nSheet: {sheet_name}")
            df = xl.parse(sheet_name)
            print(df.to_string())
    except Exception as e:
        print(f"Error reading {filename}: {e}")

# Read Lean Canvas
read_pdf("LEAN CANVAS - SIGECLIN.pdf")

# Read Requirements
read_excel("LISTAS RF y RNF - V03.xlsx")

# Read Process Matrix
read_excel("MATRIZ DE PROCESOS  - v03.xlsx")
