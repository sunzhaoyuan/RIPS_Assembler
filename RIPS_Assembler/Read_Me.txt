RIPS_Assembler User manual
Use command line run Assembler.jar.
Enter the correct file path.
It will automatically write a txt file named “your file’s name” + “_Output” in the same directory.

Please Note:
1. every value should have prefix. (d: Decimal; b: binary; h: hex).
2. it outputs hex.
3. Label must be in seperate single line.

[Sample code:]
relPrime:
loadsp d-1
storesp d1
andi d0
WhileLoop:
addi d2
storesp d2
addsp d4
jal gcd
addsp d-4
loadsp d3
addi d-1
bnez WhileLoop
addi d1
storesp d-1
loadsp d0
ja d0
gcd:
loadsp d-2
storesp d1
loadsp d-3
storesp d-1
bnez Loop2
loadsp d1
storesp d-1
loadsp d0
ja d0
Loop2:
loadsp d1
bez End
sltsp d2
bnez Else
loadsp d2
submsp d1
storesp d-1
andi d0
bez There
Else:
loadsp d1
submsp d-1
storesp d1
andi d0
There:
bez Loop2
End:
loadsp d0
ja d0