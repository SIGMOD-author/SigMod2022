file1 = open('pruneWithMask.txt', 'r') 
lines = file1.readlines() 
res = []
for str in lines:
  for s in str.split():
    if s.isdigit():
      res.append(int(s))
print(res)
