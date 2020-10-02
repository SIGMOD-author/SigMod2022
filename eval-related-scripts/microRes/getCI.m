function CI95 = getCI(input)
N = length(input);
SEM = std(input) / sqrt(N);     
CI95 = SEM * tinv(0.975, N-1);
end