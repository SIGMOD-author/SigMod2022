f = figure;
yyaxis left;
plot([0.5, 0.6, 0.7, 0.8, 0.9, 1], [27.7378 10.4724 5.0143 0.9785 0.0951 0], '-o');
xlabel("Threshold parameter");
ylabel("Approximation error (%)");
ylim([0,50])
hold on;
yyaxis right;
plot([0.5, 0.6, 0.7, 0.8, 0.9, 1], flip(timetrend * 100), '-*');
ylabel("Normalized Computation time (%)");
set(gca, 'XTick', [0.5, 0.6, 0.7, 0.8, 0.9, 1]);
plot([0.6, 0.6], [0, 100], '--')
save_plot_as_twoy(f, 'micro-fastemd');