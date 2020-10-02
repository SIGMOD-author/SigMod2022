%% CDF plot
load('autosplit.mat');
auto = sort(auto);
equal = {};
equal{1} = equal1;
equal{2} = equal2;
equal{3} = equal3;
equal{4} = equal4;
equal{5} = equal5;
equal{6} = equal6;
equal{7} = equal7;
equal{8} = equal8;
equal{9} = equal9;
equal{10} = equal10;
f = figure;
oracle = sort(oracle);
h = cdfplot(oracle);
set(h, 'LineStyle', '--');
hold on;
cdfplot(auto);
h = cdfplot(equal{1});
set(h, 'Marker', '*');
h = cdfplot(equal{5});
set(h, 'Marker', 'o');
h = cdfplot(equal{10});
set(h, 'Marker', 's');
xlim([0,0.8]);
legend('oracle', 'auto', 'equal-1', 'equal-5', 'equal-10')
xlabel("OMD");
ylabel("CDF over SVSs");
title('')
save_plot_as(f, "micro-autosplit-cdf");
%% Average plot
f = figure;
bar([1 : 5],[mean(oracle); mean(auto); mean(equal{10}); mean(equal{5}); mean(equal{1})]);
hold on;
errorbar([mean(oracle); mean(auto); mean(equal{10}); mean(equal{5}); mean(equal{1})], [getCI(oracle), getCI(auto), getCI(equal{10}), getCI(equal{5}), getCI(equal{1})]);
ylabel("Average OMD");
xlabel("Type of splitting method");
set(gca, 'XTickLabel', {'oracle', 'auto', 'equal-10', 'equal-5', 'equal-1'})
save_plot_as(f, "micro-autosplit-avg");