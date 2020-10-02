dim = 1024;
%% Generate synthesized data
r = zeros(1, dim);
total = [];
for i = 1 : 1000
    if mod(i, 100) == 1
        r = r + rand(1, dim);
        r = r / norm(r);
    end
    Sigma = diag(rand(1,dim) / 1000);
    size = 500;
    f = mvnrnd(r, Sigma, size);
    f = normalize(f', 'norm');
    f = f';
    norm(f(123,:))
    filename = ['synthesized/s', int2str(i),'.csv'];
    csvwrite(filename, f(:));
end
