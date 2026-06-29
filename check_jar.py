import zipfile, os

path = 'spark/target/ids-spark-1.0.0.jar'
size = os.path.getsize(path)
with open(path, 'rb') as f:
    data = f.read()

print(f'File size: {len(data)}')
print(f'Header: {data[:4]}')
print(f'End (last 22): {data[-22:].hex()}')

try:
    with zipfile.ZipFile(path) as z:
        names = z.namelist()
        print(f'Total entries: {len(names)}')
        for n in names:
            if 'com/ids' in n or 'META-INF/MANIFEST' in n:
                print(f'  {n}')
except Exception as e:
    print(f'Error: {e}')
