import React from 'react';
import { Link } from 'react-router-dom';
import { getCurrentUser } from '../services/auth';
import { getDefaultRouteForUser } from '../services/permissions';
import './MarketingHomePage.css';

const featureHighlights = [
  {
    eyebrow: 'Web Setup',
    title: 'Build the operating model before automation starts.',
    description:
      'Create organizations and teams, connect ServiceNow, configure watched assignment groups, and keep each team’s routing setup independent.',
  },
  {
    eyebrow: 'Access Control',
    title: 'Manage users across teams without duplicate accounts.',
    description:
      'Invite users into the organization, assign team roles, move people across teams, and support account deletion workflows when someone leaves.',
  },
  {
    eyebrow: 'CI Ownership Mapping',
    title: 'Route incidents to the right owners in the right order.',
    description:
      'Map each configuration item to the users who support it. Add multiple owners to one CI and arrange them in the exact order InciTeam should follow for round-robin assignment.',
  },
  {
    eyebrow: 'Coverage Planning',
    title: 'Build coverage across geos, shifts, schedules, leaves, and breaks.',
    description:
      'Create geos, shifts, and schedules, then layer planned leave and short breaks so routing avoids people who are unavailable.',
  },
  {
    eyebrow: 'Roster View',
    title: 'See daily and weekly availability with clear status colors.',
    description:
      'The roster shows green for available people, red for leave across the whole leave range, and yellow for break windows.',
  },
  {
    eyebrow: 'Notifications',
    title: 'Choose which operational events notify the team.',
    description:
      'Teams can select alerts for assignment success, skipped assignment, unsupported CI, and ServiceNow poller failures, with email delivery through AWS SES SMTP.',
  },
  {
    eyebrow: 'iOS Companion',
    title: 'Stay close to operations from a native iOS app.',
    description:
      'The iOS app supports sign-in, roster and schedule views, summary health, logs, diagnostics, quick leave or break entries, account deletion, and APNs assignment alerts.',
  },
  {
    eyebrow: 'Smart Assignment',
    title: 'Check ownership, availability, and critical workload before assigning.',
    description:
      'Before assigning, InciTeam checks CI ownership, active geo and shift, schedules, breaks, leaves, and whether the selected owner is already handling P0/P1C work.',
  },
  {
    eyebrow: 'Round-Robin Distribution',
    title: 'Distribute work fairly across mapped owners.',
    description:
      'For each CI, InciTeam follows the configured owner order and rotates assignments so one person does not become the default owner.',
  },
  {
    eyebrow: 'Polling and Logs',
    title: 'Track every poll and retain recent operational history.',
    description:
      'Automatic polling and manual Poll Now are recorded with assignment selections, results, skips, failures, and a 30-day log retention window.',
  },
  {
    eyebrow: 'Diagnostics and Summary',
    title: 'Validate setup and focus attention fast.',
    description:
      'Dry-run diagnostics explain assignment decisions, while Summary highlights connection health, latest poll results, validation risks, coverage gaps, and handoff items.',
  },
];

const notificationEvents = [
  'Assignment completed',
  'Eligible owners busy or unavailable',
  'Unsupported CI incident',
  'ServiceNow connection or poller issue',
];

const mobileFeatures = [
  'Roster and schedule visibility',
  'Latest poll and assignment logs',
  'Manual poll now for authorized users',
  'Quick break and leave entries',
  'APNs incident assignment alerts',
  'Account settings and deletion',
];

export default function MarketingHomePage() {
  const currentUser = getCurrentUser();
  const primaryPath = currentUser ? getDefaultRouteForUser(currentUser) : '/signup';
  const primaryLabel = currentUser ? 'Open App' : 'Create Organization';
  const authLinkProps = currentUser ? {} : { target: '_blank', rel: 'noreferrer' };

  return (
    <div className="marketing-page">
      <header className="marketing-nav">
        <div className="marketing-nav__brand">
          <div className="marketing-nav__eyebrow">Incident and Team Management Platform for Service Now</div>
          <div className="marketing-nav__name">InciTeam</div>
        </div>
        <nav className="marketing-nav__links">
          <a href="#why">Features</a>
          <a href="#operations">Operations</a>
          <a href="#mobile">iOS</a>
          <a href="#workflow">How It Works</a>
          <Link to="/privacy">Privacy</Link>
        </nav>
      </header>

      <main className="marketing-main">
        <section className="marketing-hero">
          <div className="marketing-hero__copy">
            <div className="marketing-tag">Web administration, mobile operations, and ServiceNow-aware routing</div>
            <h1>
              Automate ServiceNow assignment with team-aware coverage.
            </h1>
            <p>
              InciTeam connects ServiceNow incidents to real team availability, CI ownership,
              notification preferences, and a native iOS companion app for day-to-day operations.
            </p>
            <div className="marketing-proof">
              <div>
                <strong>Coverage-aware assignment</strong>
                <span>Route incidents using CI ownership, active shift, schedules, leave, breaks, and P0/P1C workload checks.</span>
              </div>
              <div>
                <strong>Alerts where work happens</strong>
                <span>Send email through AWS SES SMTP, support APNs assignment alerts, and keep Slack/email setup attached to each team.</span>
              </div>
            </div>
          </div>

          <div className="marketing-hero__panel">
            <div className="marketing-console">
              <div className="marketing-console__toolbar">
                <span />
                <span />
                <span />
              </div>
              <div className="marketing-console__title">InciTeam Operations View</div>
              <div className="marketing-decision-card">
                <div className="marketing-console__label">Incident received</div>
                <div className="marketing-decision-card__incident">INC0010042</div>
                <div className="marketing-decision-card__meta">
                  <span>CI: Sales Force Automation</span>
                  <span>Group: Software</span>
                </div>
              </div>
              <div className="marketing-decision-flow">
                {[
                  'CI owner list found',
                  'Active geo and shift checked',
                  'Leave and break checked',
                  'P0/P1C workload checked',
                  'Email and push alerts evaluated',
                ].map((step, index) => (
                  <div className="marketing-decision-step" key={step}>
                    <span>{index + 1}</span>
                    <strong>{step}</strong>
                  </div>
                ))}
              </div>
              <div className="marketing-decision-result">
                <div>
                  <div className="marketing-console__label">Result</div>
                  <strong>Assigned to Ava King</strong>
                  <span>Decision logged for 30 days and pushed to iOS</span>
                </div>
              </div>
            </div>
          </div>
        </section>

        <section className="marketing-snapshot" aria-label="InciTeam platform snapshot">
          <div className="marketing-snapshot__metric">
            <strong>Web</strong>
            <span>Setup, access, routing, diagnostics</span>
          </div>
          <div className="marketing-snapshot__metric">
            <strong>iOS</strong>
            <span>Roster, logs, quick actions, push</span>
          </div>
          <div className="marketing-snapshot__metric">
            <strong>30 days</strong>
            <span>ServiceNow run log retention</span>
          </div>
          <div className="marketing-snapshot__metric">
            <strong>SES SMTP</strong>
            <span>Production email notifications</span>
          </div>
        </section>

        <section className="marketing-section" id="why">
          <div className="marketing-section__heading">
            <div className="marketing-section__eyebrow">Key Features</div>
            <h2>Everything teams need to understand coverage, automate assignment, notify responders, and explain decisions.</h2>
            <p>
              InciTeam is designed to reduce manual coordination during incident operations by connecting ownership,
              schedules, availability, notifications, mobile response, and decision insight in one place.
            </p>
          </div>
          <div className="marketing-pillars">
            {featureHighlights.map((pillar) => (
              <article key={pillar.title} className="marketing-pillar">
                <div className="marketing-pillar__eyebrow">{pillar.eyebrow}</div>
                <h3>{pillar.title}</h3>
                <p>{pillar.description}</p>
              </article>
            ))}
          </div>
        </section>

        <section className="marketing-section marketing-section--operations" id="operations">
          <div className="marketing-section__heading">
            <div className="marketing-section__eyebrow">Operational Controls</div>
            <h2>Alerts, logs, privacy, and routing controls are part of the product surface.</h2>
            <p>
              Setup does not stop at ServiceNow credentials. Teams can choose the events they care about,
              test email delivery, inspect assignment history, and keep user data lifecycle controls visible.
            </p>
          </div>
          <div className="marketing-operations">
            <article className="marketing-operation">
              <div className="marketing-operation__label">Notification Rules</div>
              <h3>Selectable scenarios per team</h3>
              <ul>
                {notificationEvents.map((event) => (
                  <li key={event}>{event}</li>
                ))}
              </ul>
            </article>
            <article className="marketing-operation">
              <div className="marketing-operation__label">Delivery Channels</div>
              <h3>Email, iOS push, and team channel setup</h3>
              <p>
                Email delivery uses AWS SES SMTP configuration with production recipient support. The iOS app
                registers APNs device tokens for assignment alerts, while team notification setup keeps Slack
                and email preferences in one place.
              </p>
            </article>
            <article className="marketing-operation">
              <div className="marketing-operation__label">Operational Evidence</div>
              <h3>Recent logs without unlimited data growth</h3>
              <p>
                ServiceNow run logs keep poll outcomes, selected assignees, skipped incidents, failures,
                and assignment confirmations for the recent 30-day operations window.
              </p>
            </article>
          </div>
        </section>

        <section className="marketing-section marketing-section--mobile" id="mobile">
          <div className="marketing-mobile">
            <div className="marketing-mobile__copy">
              <div className="marketing-section__eyebrow">Native iOS Companion</div>
              <h2>Keep the web app as command center and use iOS for live operations.</h2>
              <p>
                The iOS app uses the same backend and account system, giving responders and managers a
                focused mobile surface after setup is complete in the web app.
              </p>
              <div className="marketing-mobile__grid">
                {mobileFeatures.map((feature) => (
                  <span key={feature}>{feature}</span>
                ))}
              </div>
            </div>
            <div className="marketing-phone" aria-label="InciTeam iOS app preview">
              <div className="marketing-phone__frame">
                <div className="marketing-phone__status" />
                <img src="/inciteam-ios-icon.png" alt="InciTeam iOS app icon" />
                <div className="marketing-phone__title">InciTeam</div>
                <div className="marketing-phone__subtitle">Demo Team A</div>
                <div className="marketing-phone__list">
                  <div>
                    <strong>Roster</strong>
                    <span>AMR / General active</span>
                  </div>
                  <div>
                    <strong>Latest Poll</strong>
                    <span>1 assigned, 0 failed</span>
                  </div>
                  <div>
                    <strong>Push Alert</strong>
                    <span>INC0010042 assigned</span>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </section>

        <section className="marketing-section marketing-section--workflow" id="workflow">
          <div className="marketing-section__heading">
            <div className="marketing-section__eyebrow">How It Works</div>
            <h2>Launch the organization, connect ServiceNow, configure notifications, and operate from web or iOS.</h2>
          </div>
          <div className="marketing-workflow">
            <div className="marketing-workflow__step">
              <span>01</span>
              <h3>Create the organization</h3>
              <p>Start with organization name and work email domain so the workspace is anchored to a real company context.</p>
            </div>
            <div className="marketing-workflow__step">
              <span>02</span>
              <h3>Connect ServiceNow first</h3>
              <p>Validate the instance URL and credentials before the rest of setup, so every team builds on a working integration.</p>
            </div>
            <div className="marketing-workflow__step">
              <span>03</span>
              <h3>Build coverage and routing</h3>
              <p>Add geos, shifts, schedules, CI ownership, and leave or break records so assignment follows real availability.</p>
            </div>
            <div className="marketing-workflow__step">
              <span>04</span>
              <h3>Operate with alerts and evidence</h3>
              <p>Use web and iOS for rosters, summaries, logs, diagnostics, notifications, and manual poll control.</p>
            </div>
          </div>
        </section>

        <section className="marketing-cta">
          <div>
            <div className="marketing-section__eyebrow">Ready To Launch</div>
            <h2>Open the product, sign in, and run ServiceNow assignment around your real teams.</h2>
          </div>
          <div className="marketing-cta__actions">
            <Link className="btn btn-primary btn-lg" to={primaryPath} {...authLinkProps}>
              {primaryLabel}
            </Link>
            <Link className="btn btn-outline-primary btn-lg" to="/signin" {...authLinkProps}>
              Sign In
            </Link>
          </div>
        </section>
      </main>

      <footer className="legal-footer legal-footer--marketing">
        <div className="legal-footer__brand">InciTeam™</div>
        <div>
          <Link to="/privacy">Privacy Policy</Link>
        </div>
        <div>Copyright © 2026 Parth Sarvan. All Rights Reserved.</div>
        <div>U.S. Trademark Application Serial No. 99808275.</div>
        <div>U.S. Copyright Case No. 1-15157770821 pending.</div>
      </footer>
    </div>
  );
}
